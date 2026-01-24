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
package com.jnzader.apigen.codegen.generator.python.security.social;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates social login (OAuth2) code for Python/FastAPI applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class PythonSocialLoginGenerator {

    /**
     * Generates social login files.
     *
     * @param providers list of OAuth2 providers (google, github, linkedin)
     * @return map of file path to content
     */
    public Map<String, String> generate(List<String> providers) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("app/core/oauth_config.py", generateOAuthConfig(providers));
        files.put("app/services/social_auth_service.py", generateService(providers));
        files.put("app/routers/social_auth.py", generateRouter(providers));
        files.put("app/schemas/social_auth.py", generateSchemas());

        return files;
    }

    private String generateOAuthConfig(List<String> providers) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                \"\"\"OAuth2 configuration for social login providers.\"\"\"

                import os
                from dataclasses import dataclass


                @dataclass
                class OAuthProvider:
                    \"\"\"OAuth2 provider configuration.\"\"\"

                    client_id: str
                    client_secret: str
                    authorize_url: str
                    token_url: str
                    userinfo_url: str
                    scopes: list[str]


                """);

        if (providers.contains("google")) {
            sb.append(
                    """
                    def get_google_config() -> OAuthProvider:
                        \"\"\"Get Google OAuth2 configuration.\"\"\"
                        return OAuthProvider(
                            client_id=os.getenv("GOOGLE_CLIENT_ID", ""),
                            client_secret=os.getenv("GOOGLE_CLIENT_SECRET", ""),
                            authorize_url="https://accounts.google.com/o/oauth2/v2/auth",
                            token_url="https://oauth2.googleapis.com/token",
                            userinfo_url="https://www.googleapis.com/oauth2/v2/userinfo",
                            scopes=["openid", "email", "profile"],
                        )


                    """);
        }

        if (providers.contains("github")) {
            sb.append(
                    """
                    def get_github_config() -> OAuthProvider:
                        \"\"\"Get GitHub OAuth2 configuration.\"\"\"
                        return OAuthProvider(
                            client_id=os.getenv("GITHUB_CLIENT_ID", ""),
                            client_secret=os.getenv("GITHUB_CLIENT_SECRET", ""),
                            authorize_url="https://github.com/login/oauth/authorize",
                            token_url="https://github.com/login/oauth/access_token",
                            userinfo_url="https://api.github.com/user",
                            scopes=["user:email"],
                        )


                    """);
        }

        if (providers.contains("linkedin")) {
            sb.append(
                    """
                    def get_linkedin_config() -> OAuthProvider:
                        \"\"\"Get LinkedIn OAuth2 configuration.\"\"\"
                        return OAuthProvider(
                            client_id=os.getenv("LINKEDIN_CLIENT_ID", ""),
                            client_secret=os.getenv("LINKEDIN_CLIENT_SECRET", ""),
                            authorize_url="https://www.linkedin.com/oauth/v2/authorization",
                            token_url="https://www.linkedin.com/oauth/v2/accessToken",
                            userinfo_url="https://api.linkedin.com/v2/userinfo",
                            scopes=["openid", "profile", "email"],
                        )


                    """);
        }

        sb.append(
                """
                OAUTH_PROVIDERS: dict[str, callable] = {
                """);

        for (String provider : providers) {
            sb.append(String.format("    \"%s\": get_%s_config,%n", provider, provider));
        }

        sb.append(
                """
                }


                def get_oauth_config(provider: str) -> OAuthProvider | None:
                    \"\"\"Get OAuth configuration for the specified provider.\"\"\"
                    config_fn = OAUTH_PROVIDERS.get(provider)
                    if config_fn:
                        return config_fn()
                    return None
                """);

        return sb.toString();
    }

    private String generateService(List<String> providers) {
        return """
        \"\"\"Social authentication service.\"\"\"

        import secrets
        from urllib.parse import urlencode
        from uuid import UUID

        import httpx
        from sqlalchemy import select
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.core.oauth_config import get_oauth_config
        from app.core.security import create_access_token, create_refresh_token
        from app.models.user import User
        from app.schemas.social_auth import OAuthCallbackResult, SocialUserInfo


        class SocialAuthService:
            \"\"\"Service for handling social authentication.\"\"\"

            def __init__(self, db: AsyncSession):
                self.db = db

            def get_authorization_url(
                self, provider: str, redirect_uri: str, state: str | None = None
            ) -> str | None:
                \"\"\"Get the authorization URL for the OAuth provider.\"\"\"
                config = get_oauth_config(provider)
                if not config:
                    return None

                state = state or secrets.token_urlsafe(32)

                params = {
                    "client_id": config.client_id,
                    "redirect_uri": redirect_uri,
                    "response_type": "code",
                    "scope": " ".join(config.scopes),
                    "state": state,
                }

                return f"{config.authorize_url}?{urlencode(params)}"

            async def handle_callback(
                self, provider: str, code: str, redirect_uri: str
            ) -> OAuthCallbackResult | None:
                \"\"\"Handle OAuth callback and return tokens.\"\"\"
                config = get_oauth_config(provider)
                if not config:
                    return None

                # Exchange code for token
                async with httpx.AsyncClient() as client:
                    token_response = await client.post(
                        config.token_url,
                        data={
                            "client_id": config.client_id,
                            "client_secret": config.client_secret,
                            "code": code,
                            "redirect_uri": redirect_uri,
                            "grant_type": "authorization_code",
                        },
                        headers={"Accept": "application/json"},
                    )

                    if token_response.status_code != 200:
                        return None

                    token_data = token_response.json()
                    access_token = token_data.get("access_token")

                    if not access_token:
                        return None

                    # Get user info
                    userinfo_response = await client.get(
                        config.userinfo_url,
                        headers={"Authorization": f"Bearer {access_token}"},
                    )

                    if userinfo_response.status_code != 200:
                        return None

                    userinfo = userinfo_response.json()

                # Parse user info based on provider
                social_user = self._parse_userinfo(provider, userinfo)
                if not social_user:
                    return None

                # Find or create user
                user = await self._find_or_create_user(social_user, provider)

                # Generate tokens
                access_token = create_access_token(data={"sub": str(user.id)})
                refresh_token = create_refresh_token(data={"sub": str(user.id)})

                return OAuthCallbackResult(
                    access_token=access_token,
                    refresh_token=refresh_token,
                    user_id=user.id,
                    email=user.email,
                    is_new_user=False,  # Updated by _find_or_create_user
                )

            def _parse_userinfo(
                self, provider: str, userinfo: dict
            ) -> SocialUserInfo | None:
                \"\"\"Parse user info from OAuth provider response.\"\"\"
                if provider == "google":
                    return SocialUserInfo(
                        provider=provider,
                        provider_id=userinfo.get("id"),
                        email=userinfo.get("email"),
                        name=userinfo.get("name"),
                        picture=userinfo.get("picture"),
                    )
                elif provider == "github":
                    return SocialUserInfo(
                        provider=provider,
                        provider_id=str(userinfo.get("id")),
                        email=userinfo.get("email"),
                        name=userinfo.get("name") or userinfo.get("login"),
                        picture=userinfo.get("avatar_url"),
                    )
                elif provider == "linkedin":
                    return SocialUserInfo(
                        provider=provider,
                        provider_id=userinfo.get("sub"),
                        email=userinfo.get("email"),
                        name=userinfo.get("name"),
                        picture=userinfo.get("picture"),
                    )
                return None

            async def _find_or_create_user(
                self, social_user: SocialUserInfo, provider: str
            ) -> User:
                \"\"\"Find existing user or create a new one.\"\"\"
                # Try to find by email
                result = await self.db.execute(
                    select(User).where(User.email == social_user.email)
                )
                user = result.scalar_one_or_none()

                if user:
                    # Update provider info
                    setattr(user, f"{provider}_id", social_user.provider_id)
                    await self.db.commit()
                    return user

                # Create new user
                user = User(
                    email=social_user.email,
                    username=social_user.name,
                    is_active=True,
                    is_verified=True,  # Social login users are auto-verified
                )
                setattr(user, f"{provider}_id", social_user.provider_id)

                self.db.add(user)
                await self.db.commit()
                await self.db.refresh(user)

                return user
        """;
    }

    private String generateRouter(List<String> providers) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                \"\"\"Social authentication router.\"\"\"

                from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
                from fastapi.responses import RedirectResponse
                from sqlalchemy.ext.asyncio import AsyncSession

                from app.core.database import get_db
                from app.schemas.social_auth import AuthUrlResponse, OAuthCallbackResult
                from app.services.social_auth_service import SocialAuthService

                router = APIRouter(prefix="/auth/social", tags=["Social Authentication"])

                SUPPORTED_PROVIDERS = [\
                """);

        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("\"%s\"", providers.get(i)));
        }

        sb.append(
                """
                ]


                @router.get("/{provider}/authorize", response_model=AuthUrlResponse)
                async def get_authorization_url(
                    provider: str,
                    request: Request,
                    db: AsyncSession = Depends(get_db),
                ) -> AuthUrlResponse:
                    \"\"\"Get the authorization URL for the specified provider.\"\"\"
                    if provider not in SUPPORTED_PROVIDERS:
                        raise HTTPException(
                            status_code=status.HTTP_400_BAD_REQUEST,
                            detail=f"Unsupported provider: {provider}",
                        )

                    service = SocialAuthService(db)
                    redirect_uri = f"{request.base_url}api/auth/social/{provider}/callback"
                    auth_url = service.get_authorization_url(provider, redirect_uri)

                    if not auth_url:
                        raise HTTPException(
                            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                            detail="Failed to generate authorization URL",
                        )

                    return AuthUrlResponse(authorization_url=auth_url, provider=provider)


                @router.get("/{provider}/callback")
                async def oauth_callback(
                    provider: str,
                    request: Request,
                    code: str = Query(...),
                    state: str = Query(None),
                    db: AsyncSession = Depends(get_db),
                ) -> OAuthCallbackResult:
                    \"\"\"Handle OAuth callback from provider.\"\"\"
                    if provider not in SUPPORTED_PROVIDERS:
                        raise HTTPException(
                            status_code=status.HTTP_400_BAD_REQUEST,
                            detail=f"Unsupported provider: {provider}",
                        )

                    service = SocialAuthService(db)
                    redirect_uri = f"{request.base_url}api/auth/social/{provider}/callback"

                    result = await service.handle_callback(provider, code, redirect_uri)

                    if not result:
                        raise HTTPException(
                            status_code=status.HTTP_401_UNAUTHORIZED,
                            detail="Authentication failed",
                        )

                    return result


                @router.get("/providers")
                async def list_providers() -> dict:
                    \"\"\"List supported OAuth providers.\"\"\"
                    return {"providers": SUPPORTED_PROVIDERS}
                """);

        return sb.toString();
    }

    private String generateSchemas() {
        return """
        \"\"\"Social authentication schemas.\"\"\"

        from uuid import UUID

        from pydantic import BaseModel, EmailStr


        class SocialUserInfo(BaseModel):
            \"\"\"User info from OAuth provider.\"\"\"

            provider: str
            provider_id: str
            email: EmailStr | None = None
            name: str | None = None
            picture: str | None = None


        class AuthUrlResponse(BaseModel):
            \"\"\"Response containing authorization URL.\"\"\"

            authorization_url: str
            provider: str


        class OAuthCallbackResult(BaseModel):
            \"\"\"Result of OAuth callback.\"\"\"

            access_token: str
            refresh_token: str
            token_type: str = "bearer"
            user_id: UUID
            email: EmailStr | None = None
            is_new_user: bool = False
        """;
    }
}
