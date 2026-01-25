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
package com.jnzader.apigen.codegen.generator.typescript.security.social;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates social login (OAuth2) code for TypeScript/NestJS applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: Provider names intentional for clarity; S3400: template methods return constants
public class TypeScriptSocialLoginGenerator {

    /**
     * Generates social login files.
     *
     * @param providers list of OAuth2 providers
     * @return map of file path to content
     */
    public Map<String, String> generate(List<String> providers) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("src/auth/social/social-auth.module.ts", generateModule(providers));
        files.put("src/auth/social/social-auth.service.ts", generateService());
        files.put("src/auth/social/social-auth.controller.ts", generateController(providers));
        files.put("src/auth/social/dto/social-auth.dto.ts", generateDto());

        if (providers.contains("google")) {
            files.put("src/auth/social/strategies/google.strategy.ts", generateGoogleStrategy());
        }
        if (providers.contains("github")) {
            files.put("src/auth/social/strategies/github.strategy.ts", generateGitHubStrategy());
        }

        return files;
    }

    private String generateModule(List<String> providers) {
        StringBuilder strategyImports = new StringBuilder();
        StringBuilder strategyProviders = new StringBuilder();

        if (providers.contains("google")) {
            strategyImports.append(
                    "import { GoogleStrategy } from './strategies/google.strategy';\n");
            strategyProviders.append("    GoogleStrategy,\n");
        }
        if (providers.contains("github")) {
            strategyImports.append(
                    "import { GitHubStrategy } from './strategies/github.strategy';\n");
            strategyProviders.append("    GitHubStrategy,\n");
        }

        return String.format(
                """
                import { Module } from '@nestjs/common';
                import { PassportModule } from '@nestjs/passport';
                import { ConfigModule } from '@nestjs/config';
                import { SocialAuthService } from './social-auth.service';
                import { SocialAuthController } from './social-auth.controller';
                import { UsersModule } from '../../users/users.module';
                import { JwtModule } from '@nestjs/jwt';
                %s

                @Module({
                  imports: [
                    PassportModule.register({ defaultStrategy: 'jwt' }),
                    ConfigModule,
                    UsersModule,
                    JwtModule,
                  ],
                  controllers: [SocialAuthController],
                  providers: [
                    SocialAuthService,
                %s  ],
                  exports: [SocialAuthService],
                })
                export class SocialAuthModule {}
                """,
                strategyImports, strategyProviders);
    }

    private String generateService() {
        return """
        import { Injectable, Logger } from '@nestjs/common';
        import { JwtService } from '@nestjs/jwt';
        import { ConfigService } from '@nestjs/config';
        import { UsersService } from '../../users/users.service';
        import { SocialUserDto, OAuthCallbackResultDto } from './dto/social-auth.dto';

        @Injectable()
        export class SocialAuthService {
          private readonly logger = new Logger(SocialAuthService.name);

          constructor(
            private readonly usersService: UsersService,
            private readonly jwtService: JwtService,
            private readonly configService: ConfigService,
          ) {}

          async validateSocialUser(profile: SocialUserDto): Promise<OAuthCallbackResultDto> {
            let user = await this.usersService.findByEmail(profile.email);
            let isNewUser = false;

            if (!user) {
              // Create new user
              user = await this.usersService.create({
                email: profile.email,
                username: profile.name,
                isActive: true,
                isVerified: true, // Social login users are auto-verified
                [`${profile.provider}Id`]: profile.providerId,
              });
              isNewUser = true;
              this.logger.log(`New user created via ${profile.provider}: ${profile.email}`);
            } else {
              // Update provider ID if not set
              const providerIdField = `${profile.provider}Id`;
              if (!user[providerIdField]) {
                await this.usersService.update(user.id, { [providerIdField]: profile.providerId });
              }
            }

            // Generate tokens
            const payload = { sub: user.id, email: user.email };
            const accessToken = this.jwtService.sign(payload, {
              secret: this.configService.get('JWT_SECRET'),
              expiresIn: this.configService.get('JWT_EXPIRATION', '15m'),
            });
            const refreshToken = this.jwtService.sign(payload, {
              secret: this.configService.get('JWT_REFRESH_SECRET'),
              expiresIn: this.configService.get('JWT_REFRESH_EXPIRATION', '7d'),
            });

            return {
              accessToken,
              refreshToken,
              tokenType: 'bearer',
              userId: user.id,
              email: user.email,
              isNewUser,
            };
          }
        }
        """;
    }

    private String generateController(List<String> providers) {
        StringBuilder routes = new StringBuilder();

        if (providers.contains("google")) {
            routes.append(
                    """

                      @Get('google')
                      @UseGuards(AuthGuard('google'))
                      @ApiOperation({ summary: 'Initiate Google OAuth login' })
                      googleAuth() {
                        // Guard redirects to Google
                      }

                      @Get('google/callback')
                      @UseGuards(AuthGuard('google'))
                      @ApiOperation({ summary: 'Google OAuth callback' })
                      async googleAuthCallback(@Req() req: Request): Promise<OAuthCallbackResultDto> {
                        return this.socialAuthService.validateSocialUser(req.user as SocialUserDto);
                      }
                    """);
        }

        if (providers.contains("github")) {
            routes.append(
                    """

                      @Get('github')
                      @UseGuards(AuthGuard('github'))
                      @ApiOperation({ summary: 'Initiate GitHub OAuth login' })
                      githubAuth() {
                        // Guard redirects to GitHub
                      }

                      @Get('github/callback')
                      @UseGuards(AuthGuard('github'))
                      @ApiOperation({ summary: 'GitHub OAuth callback' })
                      async githubAuthCallback(@Req() req: Request): Promise<OAuthCallbackResultDto> {
                        return this.socialAuthService.validateSocialUser(req.user as SocialUserDto);
                      }
                    """);
        }

        return String.format(
                """
                import { Controller, Get, UseGuards, Req } from '@nestjs/common';
                import { AuthGuard } from '@nestjs/passport';
                import { ApiTags, ApiOperation } from '@nestjs/swagger';
                import { Request } from 'express';
                import { SocialAuthService } from './social-auth.service';
                import { SocialUserDto, OAuthCallbackResultDto } from './dto/social-auth.dto';

                @ApiTags('Social Authentication')
                @Controller('auth/social')
                export class SocialAuthController {
                  constructor(private readonly socialAuthService: SocialAuthService) {}

                  @Get('providers')
                  @ApiOperation({ summary: 'List supported OAuth providers' })
                  getProviders(): { providers: string[] } {
                    return { providers: %s };
                  }
                %s}
                """,
                providers.toString().replace("[", "['").replace("]", "']").replace(", ", "', '"),
                routes);
    }

    private String generateDto() {
        return """
        import { ApiProperty } from '@nestjs/swagger';

        export class SocialUserDto {
          @ApiProperty()
          provider: string;

          @ApiProperty()
          providerId: string;

          @ApiProperty()
          email: string;

          @ApiProperty({ required: false })
          name?: string;

          @ApiProperty({ required: false })
          picture?: string;
        }

        export class OAuthCallbackResultDto {
          @ApiProperty()
          accessToken: string;

          @ApiProperty()
          refreshToken: string;

          @ApiProperty({ default: 'bearer' })
          tokenType: string;

          @ApiProperty()
          userId: string;

          @ApiProperty()
          email: string;

          @ApiProperty()
          isNewUser: boolean;
        }

        export class AuthUrlResponseDto {
          @ApiProperty()
          authorizationUrl: string;

          @ApiProperty()
          provider: string;
        }
        """;
    }

    private String generateGoogleStrategy() {
        return """
        import { Injectable } from '@nestjs/common';
        import { PassportStrategy } from '@nestjs/passport';
        import { Strategy, VerifyCallback } from 'passport-google-oauth20';
        import { ConfigService } from '@nestjs/config';
        import { SocialUserDto } from '../dto/social-auth.dto';

        @Injectable()
        export class GoogleStrategy extends PassportStrategy(Strategy, 'google') {
          constructor(configService: ConfigService) {
            super({
              clientID: configService.get('GOOGLE_CLIENT_ID'),
              clientSecret: configService.get('GOOGLE_CLIENT_SECRET'),
              callbackURL: configService.get('GOOGLE_CALLBACK_URL', '/auth/social/google/callback'),
              scope: ['email', 'profile'],
            });
          }

          async validate(
            accessToken: string,
            refreshToken: string,
            profile: any,
            done: VerifyCallback,
          ): Promise<any> {
            const { id, emails, displayName, photos } = profile;

            const user: SocialUserDto = {
              provider: 'google',
              providerId: id,
              email: emails[0].value,
              name: displayName,
              picture: photos?.[0]?.value,
            };

            done(null, user);
          }
        }
        """;
    }

    private String generateGitHubStrategy() {
        return """
        import { Injectable } from '@nestjs/common';
        import { PassportStrategy } from '@nestjs/passport';
        import { Strategy } from 'passport-github2';
        import { ConfigService } from '@nestjs/config';
        import { SocialUserDto } from '../dto/social-auth.dto';

        @Injectable()
        export class GitHubStrategy extends PassportStrategy(Strategy, 'github') {
          constructor(configService: ConfigService) {
            super({
              clientID: configService.get('GITHUB_CLIENT_ID'),
              clientSecret: configService.get('GITHUB_CLIENT_SECRET'),
              callbackURL: configService.get('GITHUB_CALLBACK_URL', '/auth/social/github/callback'),
              scope: ['user:email'],
            });
          }

          async validate(
            accessToken: string,
            refreshToken: string,
            profile: any,
            done: Function,
          ): Promise<any> {
            const { id, emails, displayName, username, photos } = profile;

            const user: SocialUserDto = {
              provider: 'github',
              providerId: id,
              email: emails?.[0]?.value || `${username}@github.com`,
              name: displayName || username,
              picture: photos?.[0]?.value,
            };

            done(null, user);
          }
        }
        """;
    }
}
