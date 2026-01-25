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
package com.jnzader.apigen.codegen.generator.python.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset flow for Python/FastAPI applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings("java:S3400") // Template methods return constants for code generation
public class PythonPasswordResetGenerator {

    /**
     * Generates password reset files.
     *
     * @param tokenExpirationMinutes token expiration time in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int tokenExpirationMinutes) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("app/models/password_reset_token.py", generateModel());
        files.put("app/schemas/password_reset.py", generateSchemas());
        files.put(
                "app/services/password_reset_service.py", generateService(tokenExpirationMinutes));
        files.put("app/routers/password_reset.py", generateRouter());

        return files;
    }

    private String generateModel() {
        return """
        \"""Password reset token model.\"""

        from datetime import datetime
        from uuid import uuid4

        from sqlalchemy import Boolean, Column, DateTime, ForeignKey, String
        from sqlalchemy.dialects.postgresql import UUID
        from sqlalchemy.orm import relationship

        from app.core.database import Base


        class PasswordResetToken(Base):
            \"""Model for storing password reset tokens.\"""

            __tablename__ = "password_reset_tokens"

            id = Column(UUID(as_uuid=True), primary_key=True, default=uuid4)
            user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
            token = Column(String(255), unique=True, nullable=False, index=True)
            expires_at = Column(DateTime, nullable=False)
            used = Column(Boolean, default=False)
            created_at = Column(DateTime, default=datetime.utcnow)

            user = relationship("User", back_populates="password_reset_tokens")

            @property
            def is_expired(self) -> bool:
                \"""Check if the token has expired.\"""
                return datetime.utcnow() > self.expires_at

            @property
            def is_valid(self) -> bool:
                \"""Check if the token is valid (not expired and not used).\"""
                return not self.is_expired and not self.used
        """;
    }

    private String generateSchemas() {
        return """
        \"""Password reset schemas.\"""

        from pydantic import BaseModel, EmailStr, Field


        class ForgotPasswordRequest(BaseModel):
            \"""Request to initiate password reset.\"""

            email: EmailStr


        class ForgotPasswordResponse(BaseModel):
            \"""Response for forgot password request.\"""

            message: str = "If the email exists, a password reset link has been sent."


        class ResetPasswordRequest(BaseModel):
            \"""Request to reset password with token.\"""

            token: str
            new_password: str = Field(..., min_length=8, max_length=128)
            confirm_password: str = Field(..., min_length=8, max_length=128)


        class ResetPasswordResponse(BaseModel):
            \"""Response for password reset.\"""

            success: bool
            message: str


        class ValidateTokenRequest(BaseModel):
            \"""Request to validate reset token.\"""

            token: str


        class ValidateTokenResponse(BaseModel):
            \"""Response for token validation.\"""

            valid: bool
            message: str | None = None
        """;
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                \"""Password reset service.\"""

                import secrets
                from datetime import datetime, timedelta
                from uuid import UUID

                from sqlalchemy import select
                from sqlalchemy.ext.asyncio import AsyncSession

                from app.models.password_reset_token import PasswordResetToken
                from app.models.user import User
                from app.services.mail_service import get_mail_service
                from app.core.security import get_password_hash


                class PasswordResetService:
                    \"""Service for handling password reset operations.\"""

                    TOKEN_EXPIRATION_MINUTES = %d

                    def __init__(self, db: AsyncSession):
                        self.db = db
                        self.mail_service = get_mail_service()

                    async def request_password_reset(self, email: str, base_url: str) -> bool:
                        \"""
                        Request a password reset for the given email.

                        Returns True if email was sent, False if user not found.
                        \"""
                        # Find user by email
                        result = await self.db.execute(
                            select(User).where(User.email == email)
                        )
                        user = result.scalar_one_or_none()

                        if not user:
                            # Return True anyway to prevent email enumeration
                            return True

                        # Invalidate existing tokens
                        await self._invalidate_existing_tokens(user.id)

                        # Generate new token
                        token = secrets.token_urlsafe(32)
                        expires_at = datetime.utcnow() + timedelta(minutes=self.TOKEN_EXPIRATION_MINUTES)

                        reset_token = PasswordResetToken(
                            user_id=user.id,
                            token=token,
                            expires_at=expires_at,
                        )
                        self.db.add(reset_token)
                        await self.db.commit()

                        # Send email
                        reset_link = f"{base_url}/reset-password?token={token}"
                        await self.mail_service.send_password_reset_email(
                            to=email,
                            user_name=user.username or user.email,
                            reset_link=reset_link,
                            expiration_minutes=self.TOKEN_EXPIRATION_MINUTES,
                        )

                        return True

                    async def validate_token(self, token: str) -> tuple[bool, str | None]:
                        \"""
                        Validate a password reset token.

                        Returns (is_valid, error_message).
                        \"""
                        result = await self.db.execute(
                            select(PasswordResetToken).where(PasswordResetToken.token == token)
                        )
                        reset_token = result.scalar_one_or_none()

                        if not reset_token:
                            return False, "Invalid or expired token"

                        if reset_token.used:
                            return False, "Token has already been used"

                        if reset_token.is_expired:
                            return False, "Token has expired"

                        return True, None

                    async def reset_password(
                        self, token: str, new_password: str
                    ) -> tuple[bool, str]:
                        \"""
                        Reset password using the provided token.

                        Returns (success, message).
                        \"""
                        # Validate token
                        is_valid, error = await self.validate_token(token)
                        if not is_valid:
                            return False, error or "Invalid token"

                        # Get token and user
                        result = await self.db.execute(
                            select(PasswordResetToken).where(PasswordResetToken.token == token)
                        )
                        reset_token = result.scalar_one()

                        result = await self.db.execute(
                            select(User).where(User.id == reset_token.user_id)
                        )
                        user = result.scalar_one()

                        # Update password
                        user.hashed_password = get_password_hash(new_password)
                        reset_token.used = True

                        await self.db.commit()

                        return True, "Password has been reset successfully"

                    async def _invalidate_existing_tokens(self, user_id: UUID) -> None:
                        \"""Mark all existing tokens for user as used.\"""
                        result = await self.db.execute(
                            select(PasswordResetToken).where(
                                PasswordResetToken.user_id == user_id,
                                PasswordResetToken.used == False,
                            )
                        )
                        tokens = result.scalars().all()

                        for token in tokens:
                            token.used = True

                        await self.db.commit()
                """,
                tokenExpirationMinutes);
    }

    private String generateRouter() {
        return """
        \"""Password reset router.\"""

        from fastapi import APIRouter, Depends, HTTPException, Request, status
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.core.database import get_db
        from app.schemas.password_reset import (
            ForgotPasswordRequest,
            ForgotPasswordResponse,
            ResetPasswordRequest,
            ResetPasswordResponse,
            ValidateTokenRequest,
            ValidateTokenResponse,
        )
        from app.services.password_reset_service import PasswordResetService

        router = APIRouter(prefix="/auth/password", tags=["Password Reset"])


        @router.post("/forgot", response_model=ForgotPasswordResponse)
        async def forgot_password(
            request: Request,
            data: ForgotPasswordRequest,
            db: AsyncSession = Depends(get_db),
        ) -> ForgotPasswordResponse:
            \"""Request a password reset email.\"""
            service = PasswordResetService(db)
            base_url = str(request.base_url).rstrip("/")
            await service.request_password_reset(data.email, base_url)

            # Always return success to prevent email enumeration
            return ForgotPasswordResponse()


        @router.post("/validate", response_model=ValidateTokenResponse)
        async def validate_token(
            data: ValidateTokenRequest,
            db: AsyncSession = Depends(get_db),
        ) -> ValidateTokenResponse:
            \"""Validate a password reset token.\"""
            service = PasswordResetService(db)
            is_valid, error = await service.validate_token(data.token)

            return ValidateTokenResponse(valid=is_valid, message=error)


        @router.post("/reset", response_model=ResetPasswordResponse)
        async def reset_password(
            data: ResetPasswordRequest,
            db: AsyncSession = Depends(get_db),
        ) -> ResetPasswordResponse:
            \"""Reset password using a valid token.\"""
            if data.new_password != data.confirm_password:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Passwords do not match",
                )

            service = PasswordResetService(db)
            success, message = await service.reset_password(data.token, data.new_password)

            if not success:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=message,
                )

            return ResetPasswordResponse(success=True, message=message)
        """;
    }
}
