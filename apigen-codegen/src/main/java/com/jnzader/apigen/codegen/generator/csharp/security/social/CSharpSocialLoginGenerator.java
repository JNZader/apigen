/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.csharp.security.social;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates OAuth2 social login for C#/ASP.NET Core applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class CSharpSocialLoginGenerator {

    private final String namespace;

    public CSharpSocialLoginGenerator(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Generates social login files.
     *
     * @param providers list of OAuth providers (google, github)
     * @return map of file path to content
     */
    public Map<String, String> generate(List<String> providers) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("Models/SocialAccount.cs", generateModel());
        files.put("DTOs/SocialAuthDtos.cs", generateDtos());
        files.put("Services/ISocialAuthService.cs", generateInterface());
        files.put("Services/SocialAuthService.cs", generateService(providers));
        files.put("Controllers/SocialAuthController.cs", generateController(providers));
        files.put("Configuration/SocialAuthConfig.cs", generateConfig(providers));

        return files;
    }

    private String generateModel() {
        return String.format(
                """
                using System.ComponentModel.DataAnnotations;

                namespace %s.Models;

                /// <summary>
                /// Social account link entity.
                /// </summary>
                public class SocialAccount
                {
                    [Key]
                    public Guid Id { get; set; }

                    [Required]
                    public Guid UserId { get; set; }

                    [Required]
                    [MaxLength(50)]
                    public string Provider { get; set; } = string.Empty;

                    [Required]
                    [MaxLength(255)]
                    public string ProviderId { get; set; } = string.Empty;

                    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

                    // Navigation
                    public virtual User? User { get; set; }
                }
                """,
                namespace);
    }

    private String generateDtos() {
        return String.format(
                """
                namespace %s.DTOs;

                /// <summary>
                /// OAuth login response.
                /// </summary>
                public record SocialLoginResponse
                {
                    public Guid UserId { get; init; }
                    public string? Email { get; init; }
                    public string? Name { get; init; }
                    public string Provider { get; init; } = string.Empty;
                    public bool IsNewUser { get; init; }
                    public string AccessToken { get; init; } = string.Empty;
                    public string? RefreshToken { get; init; }
                }

                /// <summary>
                /// OAuth callback request.
                /// </summary>
                public record SocialCallbackRequest
                {
                    public string Code { get; init; } = string.Empty;
                    public string? State { get; init; }
                }
                """,
                namespace);
    }

    private String generateInterface() {
        return String.format(
                """
                using %s.DTOs;

                namespace %s.Services;

                /// <summary>
                /// Social authentication service interface.
                /// </summary>
                public interface ISocialAuthService
                {
                    /// <summary>
                    /// Get authorization URL for a provider.
                    /// </summary>
                    string GetAuthorizationUrl(string provider, string? state = null);

                    /// <summary>
                    /// Handle OAuth callback.
                    /// </summary>
                    Task<SocialLoginResponse> HandleCallbackAsync(string provider, string code, string? state);
                }
                """,
                namespace, namespace);
    }

    private String generateService(List<String> providers) {
        StringBuilder providerCases = new StringBuilder();

        for (String provider : providers) {
            String capitalized = capitalize(provider);
            providerCases.append(
                    String.format(
                            """
                                        "%s" => _httpClientFactory.CreateClient("%sOAuth"),
                            """,
                            provider, capitalized));
        }

        return String.format(
                """
                using System.Net.Http.Headers;
                using System.Text.Json;
                using Microsoft.EntityFrameworkCore;
                using Microsoft.Extensions.Options;
                using %1$s.Configuration;
                using %1$s.Data;
                using %1$s.DTOs;
                using %1$s.Models;

                namespace %1$s.Services;

                /// <summary>
                /// Social authentication service implementation.
                /// </summary>
                public class SocialAuthService : ISocialAuthService
                {
                    private readonly ApplicationDbContext _context;
                    private readonly IHttpClientFactory _httpClientFactory;
                    private readonly SocialAuthSettings _settings;
                    private readonly IJwtService _jwtService;
                    private readonly ILogger<SocialAuthService> _logger;

                    public SocialAuthService(
                        ApplicationDbContext context,
                        IHttpClientFactory httpClientFactory,
                        IOptions<SocialAuthSettings> settings,
                        IJwtService jwtService,
                        ILogger<SocialAuthService> logger)
                    {
                        _context = context;
                        _httpClientFactory = httpClientFactory;
                        _settings = settings.Value;
                        _jwtService = jwtService;
                        _logger = logger;
                    }

                    /// <inheritdoc />
                    public string GetAuthorizationUrl(string provider, string? state = null)
                    {
                        var config = GetProviderConfig(provider);
                        var stateParam = state ?? Guid.NewGuid().ToString();

                        return provider.ToLower() switch
                        {
                            "google" => $"https://accounts.google.com/o/oauth2/v2/auth" +
                                $"?client_id={config.ClientId}" +
                                $"&redirect_uri={Uri.EscapeDataString(config.RedirectUri)}" +
                                $"&response_type=code" +
                                $"&scope={Uri.EscapeDataString("openid email profile")}" +
                                $"&state={stateParam}",

                            "github" => $"https://github.com/login/oauth/authorize" +
                                $"?client_id={config.ClientId}" +
                                $"&redirect_uri={Uri.EscapeDataString(config.RedirectUri)}" +
                                $"&scope=user:email" +
                                $"&state={stateParam}",

                            _ => throw new ArgumentException($"Unknown provider: {provider}")
                        };
                    }

                    /// <inheritdoc />
                    public async Task<SocialLoginResponse> HandleCallbackAsync(string provider, string code, string? state)
                    {
                        var config = GetProviderConfig(provider);

                        // Exchange code for token
                        var accessToken = await ExchangeCodeForTokenAsync(provider, code, config);

                        // Get user info
                        var (providerId, email, name, avatar) = await GetUserInfoAsync(provider, accessToken);

                        // Find or create user
                        var (user, isNew) = await FindOrCreateUserAsync(provider, providerId, email, name, avatar);

                        // Generate JWT
                        var token = _jwtService.GenerateToken(user);

                        _logger.LogInformation("Social login successful: {Provider}, UserId: {UserId}", provider, user.Id);

                        return new SocialLoginResponse
                        {
                            UserId = user.Id,
                            Email = user.Email,
                            Name = user.Username,
                            Provider = provider,
                            IsNewUser = isNew,
                            AccessToken = token
                        };
                    }

                    private ProviderConfig GetProviderConfig(string provider)
                    {
                        return provider.ToLower() switch
                        {
                            "google" => _settings.Google ?? throw new InvalidOperationException("Google OAuth not configured"),
                            "github" => _settings.GitHub ?? throw new InvalidOperationException("GitHub OAuth not configured"),
                            _ => throw new ArgumentException($"Unknown provider: {provider}")
                        };
                    }

                    private async Task<string> ExchangeCodeForTokenAsync(string provider, string code, ProviderConfig config)
                    {
                        using var client = _httpClientFactory.CreateClient();

                        var tokenEndpoint = provider.ToLower() switch
                        {
                            "google" => "https://oauth2.googleapis.com/token",
                            "github" => "https://github.com/login/oauth/access_token",
                            _ => throw new ArgumentException($"Unknown provider: {provider}")
                        };

                        var request = new HttpRequestMessage(HttpMethod.Post, tokenEndpoint);
                        request.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));

                        var parameters = new Dictionary<string, string>
                        {
                            ["client_id"] = config.ClientId,
                            ["client_secret"] = config.ClientSecret,
                            ["code"] = code,
                            ["redirect_uri"] = config.RedirectUri,
                            ["grant_type"] = "authorization_code"
                        };

                        request.Content = new FormUrlEncodedContent(parameters);

                        var response = await client.SendAsync(request);
                        response.EnsureSuccessStatusCode();

                        var json = await response.Content.ReadAsStringAsync();
                        var doc = JsonDocument.Parse(json);

                        return doc.RootElement.GetProperty("access_token").GetString()
                            ?? throw new InvalidOperationException("No access token in response");
                    }

                    private async Task<(string ProviderId, string? Email, string? Name, string? Avatar)> GetUserInfoAsync(
                        string provider, string accessToken)
                    {
                        using var client = _httpClientFactory.CreateClient();
                        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", accessToken);

                        var userInfoEndpoint = provider.ToLower() switch
                        {
                            "google" => "https://www.googleapis.com/oauth2/v2/userinfo",
                            "github" => "https://api.github.com/user",
                            _ => throw new ArgumentException($"Unknown provider: {provider}")
                        };

                        if (provider.ToLower() == "github")
                        {
                            client.DefaultRequestHeaders.UserAgent.ParseAdd("APiGen-App");
                        }

                        var response = await client.GetAsync(userInfoEndpoint);
                        response.EnsureSuccessStatusCode();

                        var json = await response.Content.ReadAsStringAsync();
                        var doc = JsonDocument.Parse(json);

                        return provider.ToLower() switch
                        {
                            "google" => (
                                doc.RootElement.GetProperty("id").GetString()!,
                                doc.RootElement.TryGetProperty("email", out var e) ? e.GetString() : null,
                                doc.RootElement.TryGetProperty("name", out var n) ? n.GetString() : null,
                                doc.RootElement.TryGetProperty("picture", out var p) ? p.GetString() : null
                            ),
                            "github" => (
                                doc.RootElement.GetProperty("id").GetInt64().ToString(),
                                doc.RootElement.TryGetProperty("email", out var e2) ? e2.GetString() : null,
                                doc.RootElement.TryGetProperty("name", out var n2) ? n2.GetString() : null,
                                doc.RootElement.TryGetProperty("avatar_url", out var a) ? a.GetString() : null
                            ),
                            _ => throw new ArgumentException($"Unknown provider: {provider}")
                        };
                    }

                    private async Task<(User User, bool IsNew)> FindOrCreateUserAsync(
                        string provider, string providerId, string? email, string? name, string? avatar)
                    {
                        // Check for existing social account
                        var existingAccount = await _context.SocialAccounts
                            .Include(sa => sa.User)
                            .FirstOrDefaultAsync(sa => sa.Provider == provider && sa.ProviderId == providerId);

                        if (existingAccount?.User != null)
                        {
                            return (existingAccount.User, false);
                        }

                        // Check if user exists by email
                        User? user = null;
                        if (!string.IsNullOrEmpty(email))
                        {
                            user = await _context.Users.FirstOrDefaultAsync(u => u.Email == email);
                        }

                        // Create new user if needed
                        if (user == null)
                        {
                            user = new User
                            {
                                Id = Guid.NewGuid(),
                                Email = email ?? $"{providerId}@{provider}.local",
                                Username = name,
                                AvatarUrl = avatar,
                                CreatedAt = DateTime.UtcNow
                            };
                            _context.Users.Add(user);
                        }

                        // Link social account
                        var socialAccount = new SocialAccount
                        {
                            Id = Guid.NewGuid(),
                            UserId = user.Id,
                            Provider = provider,
                            ProviderId = providerId
                        };
                        _context.SocialAccounts.Add(socialAccount);

                        await _context.SaveChangesAsync();

                        return (user, true);
                    }
                }
                """,
                namespace);
    }

    private String generateController(List<String> providers) {
        StringBuilder endpoints = new StringBuilder();

        for (String provider : providers) {
            String capitalized = capitalize(provider);
            endpoints.append(
                    String.format(
                            """

                                /// <summary>
                                /// Initiate %1$s OAuth login.
                                /// </summary>
                                [HttpGet("%2$s")]
                                public IActionResult %1$sLogin([FromQuery] string? state = null)
                                {
                                    var authUrl = _socialAuthService.GetAuthorizationUrl("%2$s", state);
                                    return Redirect(authUrl);
                                }

                                /// <summary>
                                /// Handle %1$s OAuth callback.
                                /// </summary>
                                [HttpGet("%2$s/callback")]
                                [ProducesResponseType(typeof(SocialLoginResponse), StatusCodes.Status200OK)]
                                public async Task<ActionResult<SocialLoginResponse>> %1$sCallback(
                                    [FromQuery] string code,
                                    [FromQuery] string? state)
                                {
                                    var response = await _socialAuthService.HandleCallbackAsync("%2$s", code, state);
                                    return Ok(response);
                                }
                            """,
                            capitalized, provider));
        }

        return String.format(
                """
                using Microsoft.AspNetCore.Mvc;
                using %1$s.DTOs;
                using %1$s.Services;

                namespace %1$s.Controllers;

                /// <summary>
                /// Social authentication endpoints.
                /// </summary>
                [ApiController]
                [Route("api/auth/social")]
                public class SocialAuthController : ControllerBase
                {
                    private readonly ISocialAuthService _socialAuthService;
                    private readonly ILogger<SocialAuthController> _logger;

                    public SocialAuthController(
                        ISocialAuthService socialAuthService,
                        ILogger<SocialAuthController> logger)
                    {
                        _socialAuthService = socialAuthService;
                        _logger = logger;
                    }
                %2$s
                }
                """,
                namespace, endpoints.toString());
    }

    private String generateConfig(List<String> providers) {
        StringBuilder providerProperties = new StringBuilder();

        for (String provider : providers) {
            String capitalized = capitalize(provider);
            providerProperties.append(
                    String.format(
                            """

                                /// <summary>
                                /// %s OAuth configuration.
                                /// </summary>
                                public ProviderConfig? %s { get; set; }
                            """,
                            capitalized, capitalized));
        }

        return String.format(
                """
                namespace %s.Configuration;

                /// <summary>
                /// Social authentication settings.
                /// </summary>
                public class SocialAuthSettings
                {
                %s
                }

                /// <summary>
                /// OAuth provider configuration.
                /// </summary>
                public class ProviderConfig
                {
                    /// <summary>
                    /// OAuth client ID.
                    /// </summary>
                    public string ClientId { get; set; } = string.Empty;

                    /// <summary>
                    /// OAuth client secret.
                    /// </summary>
                    public string ClientSecret { get; set; } = string.Empty;

                    /// <summary>
                    /// OAuth redirect URI.
                    /// </summary>
                    public string RedirectUri { get; set; } = string.Empty;
                }
                """,
                namespace, providerProperties.toString());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
