package com.jnzader.apigen.codegen.generator.python.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Python/FastAPI JWT authentication functionality.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>JWT handler for token creation and validation
 *   <li>JWT bearer dependency for protected routes
 *   <li>Auth models (TokenPayload, TokenResponse)
 *   <li>Auth router with login/refresh endpoints
 *   <li>Configuration settings for JWT
 * </ul>
 */
public class PythonJwtAuthGenerator {

    private static final int DEFAULT_ACCESS_TOKEN_EXPIRE_MINUTES = 30;
    private static final int DEFAULT_REFRESH_TOKEN_EXPIRE_DAYS = 7;

    /**
     * Generates all JWT authentication files.
     *
     * @param accessTokenExpireMinutes access token expiration in minutes
     * @param refreshTokenExpireDays refresh token expiration in days
     * @return map of file path to content
     */
    public Map<String, String> generate(int accessTokenExpireMinutes, int refreshTokenExpireDays) {
        Map<String, String> files = new LinkedHashMap<>();

        // Auth package
        files.put("app/auth/__init__.py", generateAuthInit());
        files.put("app/auth/jwt_handler.py", generateJwtHandler(accessTokenExpireMinutes));
        files.put(
                "app/auth/jwt_bearer.py",
                generateJwtBearer(accessTokenExpireMinutes, refreshTokenExpireDays));
        files.put("app/auth/models.py", generateAuthModels());
        files.put("app/auth/router.py", generateAuthRouter());
        files.put("app/auth/service.py", generateAuthService());

        // User model for authentication
        files.put("app/models/user.py", generateUserModel());
        files.put("app/schemas/user.py", generateUserSchemas());
        files.put("app/repositories/user_repository.py", generateUserRepository());

        // Security settings
        files.put("app/core/security.py", generateSecurityModule());

        return files;
    }

    /** Generates with default expiration times. */
    public Map<String, String> generate() {
        return generate(DEFAULT_ACCESS_TOKEN_EXPIRE_MINUTES, DEFAULT_REFRESH_TOKEN_EXPIRE_DAYS);
    }

    private String generateAuthInit() {
        return """
        from app.auth.jwt_handler import create_access_token, create_refresh_token, decode_token
        from app.auth.jwt_bearer import JWTBearer, get_current_user
        from app.auth.models import TokenPayload, TokenResponse, LoginRequest
        from app.auth.service import AuthService

        __all__ = [
            "create_access_token",
            "create_refresh_token",
            "decode_token",
            "JWTBearer",
            "get_current_user",
            "TokenPayload",
            "TokenResponse",
            "LoginRequest",
            "AuthService",
        ]
        """;
    }

    private String generateJwtHandler(int accessTokenExpireMinutes) {
        return """
        from datetime import datetime, timedelta, timezone
        from typing import Optional

        from jose import JWTError, jwt
        from pydantic import ValidationError

        from app.auth.models import TokenPayload
        from app.core.config import settings


        ALGORITHM = "HS256"


        def create_access_token(
            subject: str | int,
            expires_delta: Optional[timedelta] = None,
            additional_claims: Optional[dict] = None,
        ) -> str:
            \"\"\"
            Create a JWT access token.

            Args:
                subject: The subject (usually user ID)
                expires_delta: Custom expiration time
                additional_claims: Additional claims to include

            Returns:
                Encoded JWT token
            \"\"\"
            if expires_delta:
                expire = datetime.now(timezone.utc) + expires_delta
            else:
                expire = datetime.now(timezone.utc) + timedelta(
                    minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES
                )

            to_encode = {
                "sub": str(subject),
                "exp": expire,
                "iat": datetime.now(timezone.utc),
                "type": "access",
            }

            if additional_claims:
                to_encode.update(additional_claims)

            return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=ALGORITHM)


        def create_refresh_token(
            subject: str | int,
            expires_delta: Optional[timedelta] = None,
        ) -> str:
            \"\"\"
            Create a JWT refresh token.

            Args:
                subject: The subject (usually user ID)
                expires_delta: Custom expiration time

            Returns:
                Encoded JWT refresh token
            \"\"\"
            if expires_delta:
                expire = datetime.now(timezone.utc) + expires_delta
            else:
                expire = datetime.now(timezone.utc) + timedelta(
                    days=settings.REFRESH_TOKEN_EXPIRE_DAYS
                )

            to_encode = {
                "sub": str(subject),
                "exp": expire,
                "iat": datetime.now(timezone.utc),
                "type": "refresh",
            }

            return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=ALGORITHM)


        def decode_token(token: str) -> Optional[TokenPayload]:
            \"\"\"
            Decode and validate a JWT token.

            Args:
                token: The JWT token to decode

            Returns:
                TokenPayload if valid, None otherwise
            \"\"\"
            try:
                payload = jwt.decode(
                    token,
                    settings.SECRET_KEY,
                    algorithms=[ALGORITHM],
                )
                return TokenPayload(**payload)
            except (JWTError, ValidationError):
                return None


        def verify_token(token: str, token_type: str = "access") -> Optional[TokenPayload]:
            \"\"\"
            Verify a token and check its type.

            Args:
                token: The JWT token to verify
                token_type: Expected token type ("access" or "refresh")

            Returns:
                TokenPayload if valid and correct type, None otherwise
            \"\"\"
            payload = decode_token(token)
            if payload is None:
                return None
            if payload.type != token_type:
                return None
            return payload
        """;
    }

    private String generateJwtBearer(int accessTokenExpireMinutes, int refreshTokenExpireDays) {
        return """
        from typing import Annotated, Optional

        from fastapi import Depends, HTTPException, status
        from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.auth.jwt_handler import verify_token
        from app.auth.models import TokenPayload
        from app.core.database import get_db
        from app.models.user import User
        from app.repositories.user_repository import UserRepository


        security = HTTPBearer()


        class JWTBearer:
            \"\"\"
            JWT Bearer authentication dependency.

            Usage:
                @router.get("/protected")
                async def protected_route(
                    current_user: Annotated[User, Depends(get_current_user)]
                ):
                    return {"user": current_user.email}
            \"\"\"

            def __init__(self, auto_error: bool = True):
                self.auto_error = auto_error

            async def __call__(
                self,
                credentials: Annotated[
                    Optional[HTTPAuthorizationCredentials], Depends(security)
                ],
            ) -> Optional[TokenPayload]:
                if credentials is None:
                    if self.auto_error:
                        raise HTTPException(
                            status_code=status.HTTP_401_UNAUTHORIZED,
                            detail="Not authenticated",
                            headers={"WWW-Authenticate": "Bearer"},
                        )
                    return None

                token = credentials.credentials
                payload = verify_token(token, token_type="access")

                if payload is None:
                    if self.auto_error:
                        raise HTTPException(
                            status_code=status.HTTP_401_UNAUTHORIZED,
                            detail="Invalid or expired token",
                            headers={"WWW-Authenticate": "Bearer"},
                        )
                    return None

                return payload


        jwt_bearer = JWTBearer()


        async def get_current_user(
            token_payload: Annotated[TokenPayload, Depends(jwt_bearer)],
            db: Annotated[AsyncSession, Depends(get_db)],
        ) -> User:
            \"\"\"
            Get the current authenticated user.

            Args:
                token_payload: The validated token payload
                db: Database session

            Returns:
                The authenticated User

            Raises:
                HTTPException: If user not found or inactive
            \"\"\"
            user_repo = UserRepository(db)
            user = await user_repo.get_by_id(int(token_payload.sub))

            if user is None:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="User not found",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            if not user.is_active:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Inactive user",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            return user


        async def get_current_active_superuser(
            current_user: Annotated[User, Depends(get_current_user)],
        ) -> User:
            \"\"\"
            Get the current authenticated superuser.

            Args:
                current_user: The current authenticated user

            Returns:
                The authenticated superuser

            Raises:
                HTTPException: If user is not a superuser
            \"\"\"
            if not current_user.is_superuser:
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="Not enough permissions",
                )
            return current_user
        """;
    }

    private String generateAuthModels() {
        return """
        from datetime import datetime
        from typing import Optional

        from pydantic import BaseModel, EmailStr, Field


        class TokenPayload(BaseModel):
            \"\"\"JWT token payload.\"\"\"

            sub: str = Field(..., description="Subject (user ID)")
            exp: datetime = Field(..., description="Expiration time")
            iat: datetime = Field(..., description="Issued at time")
            type: str = Field(..., description="Token type (access/refresh)")


        class TokenResponse(BaseModel):
            \"\"\"Response containing access and refresh tokens.\"\"\"

            access_token: str = Field(..., description="JWT access token")
            refresh_token: str = Field(..., description="JWT refresh token")
            token_type: str = Field(default="bearer", description="Token type")


        class LoginRequest(BaseModel):
            \"\"\"Login request with email and password.\"\"\"

            email: EmailStr = Field(..., description="User email")
            password: str = Field(..., min_length=8, description="User password")


        class RefreshTokenRequest(BaseModel):
            \"\"\"Request to refresh access token.\"\"\"

            refresh_token: str = Field(..., description="Valid refresh token")


        class RegisterRequest(BaseModel):
            \"\"\"User registration request.\"\"\"

            email: EmailStr = Field(..., description="User email")
            password: str = Field(..., min_length=8, max_length=100, description="User password")
            full_name: Optional[str] = Field(None, max_length=255, description="User's full name")


        class ChangePasswordRequest(BaseModel):
            \"\"\"Request to change password.\"\"\"

            current_password: str = Field(..., description="Current password")
            new_password: str = Field(..., min_length=8, max_length=100, description="New password")
        """;
    }

    private String generateAuthRouter() {
        return """
        from typing import Annotated

        from fastapi import APIRouter, Depends, HTTPException, status
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.auth.jwt_bearer import get_current_user
        from app.auth.models import (
            ChangePasswordRequest,
            LoginRequest,
            RefreshTokenRequest,
            RegisterRequest,
            TokenResponse,
        )
        from app.auth.service import AuthService
        from app.core.database import get_db
        from app.models.user import User
        from app.schemas.user import UserResponse


        router = APIRouter(prefix="/auth", tags=["Authentication"])


        @router.post("/login", response_model=TokenResponse)
        async def login(
            request: LoginRequest,
            db: Annotated[AsyncSession, Depends(get_db)],
        ) -> TokenResponse:
            \"\"\"
            Authenticate user and return tokens.

            Args:
                request: Login credentials
                db: Database session

            Returns:
                Access and refresh tokens
            \"\"\"
            auth_service = AuthService(db)
            result = await auth_service.authenticate(request.email, request.password)

            if result is None:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid email or password",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            return result


        @router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
        async def register(
            request: RegisterRequest,
            db: Annotated[AsyncSession, Depends(get_db)],
        ) -> User:
            \"\"\"
            Register a new user.

            Args:
                request: Registration data
                db: Database session

            Returns:
                Created user
            \"\"\"
            auth_service = AuthService(db)

            try:
                user = await auth_service.register(
                    email=request.email,
                    password=request.password,
                    full_name=request.full_name,
                )
                return user
            except ValueError as e:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=str(e),
                )


        @router.post("/refresh", response_model=TokenResponse)
        async def refresh_token(
            request: RefreshTokenRequest,
            db: Annotated[AsyncSession, Depends(get_db)],
        ) -> TokenResponse:
            \"\"\"
            Refresh access token using refresh token.

            Args:
                request: Refresh token
                db: Database session

            Returns:
                New access and refresh tokens
            \"\"\"
            auth_service = AuthService(db)
            result = await auth_service.refresh_tokens(request.refresh_token)

            if result is None:
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="Invalid or expired refresh token",
                    headers={"WWW-Authenticate": "Bearer"},
                )

            return result


        @router.get("/me", response_model=UserResponse)
        async def get_current_user_info(
            current_user: Annotated[User, Depends(get_current_user)],
        ) -> User:
            \"\"\"
            Get current authenticated user information.

            Args:
                current_user: The authenticated user

            Returns:
                User information
            \"\"\"
            return current_user


        @router.post("/change-password", status_code=status.HTTP_204_NO_CONTENT)
        async def change_password(
            request: ChangePasswordRequest,
            current_user: Annotated[User, Depends(get_current_user)],
            db: Annotated[AsyncSession, Depends(get_db)],
        ) -> None:
            \"\"\"
            Change current user's password.

            Args:
                request: Password change data
                current_user: The authenticated user
                db: Database session
            \"\"\"
            auth_service = AuthService(db)

            success = await auth_service.change_password(
                user=current_user,
                current_password=request.current_password,
                new_password=request.new_password,
            )

            if not success:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Invalid current password",
                )
        """;
    }

    private String generateAuthService() {
        return """
        from typing import Optional

        from sqlalchemy.ext.asyncio import AsyncSession

        from app.auth.jwt_handler import (
            create_access_token,
            create_refresh_token,
            verify_token,
        )
        from app.auth.models import TokenResponse
        from app.core.security import hash_password, verify_password
        from app.models.user import User
        from app.repositories.user_repository import UserRepository


        class AuthService:
            \"\"\"Authentication service for user login, registration, and token management.\"\"\"

            def __init__(self, db: AsyncSession):
                self.db = db
                self.user_repo = UserRepository(db)

            async def authenticate(
                self, email: str, password: str
            ) -> Optional[TokenResponse]:
                \"\"\"
                Authenticate user with email and password.

                Args:
                    email: User email
                    password: User password

                Returns:
                    TokenResponse if authentication successful, None otherwise
                \"\"\"
                user = await self.user_repo.get_by_email(email)

                if user is None:
                    return None

                if not verify_password(password, user.hashed_password):
                    return None

                if not user.is_active:
                    return None

                return self._create_tokens(user)

            async def register(
                self,
                email: str,
                password: str,
                full_name: Optional[str] = None,
            ) -> User:
                \"\"\"
                Register a new user.

                Args:
                    email: User email
                    password: User password
                    full_name: User's full name

                Returns:
                    Created user

                Raises:
                    ValueError: If email already exists
                \"\"\"
                existing = await self.user_repo.get_by_email(email)
                if existing:
                    raise ValueError("Email already registered")

                user = User(
                    email=email,
                    hashed_password=hash_password(password),
                    full_name=full_name,
                    is_active=True,
                    is_superuser=False,
                )

                return await self.user_repo.create(user)

            async def refresh_tokens(
                self, refresh_token: str
            ) -> Optional[TokenResponse]:
                \"\"\"
                Refresh access and refresh tokens.

                Args:
                    refresh_token: Valid refresh token

                Returns:
                    New TokenResponse if valid, None otherwise
                \"\"\"
                payload = verify_token(refresh_token, token_type="refresh")

                if payload is None:
                    return None

                user = await self.user_repo.get_by_id(int(payload.sub))

                if user is None or not user.is_active:
                    return None

                return self._create_tokens(user)

            async def change_password(
                self,
                user: User,
                current_password: str,
                new_password: str,
            ) -> bool:
                \"\"\"
                Change user password.

                Args:
                    user: User to update
                    current_password: Current password for verification
                    new_password: New password to set

                Returns:
                    True if password changed successfully, False otherwise
                \"\"\"
                if not verify_password(current_password, user.hashed_password):
                    return False

                user.hashed_password = hash_password(new_password)
                await self.user_repo.update(user)

                return True

            def _create_tokens(self, user: User) -> TokenResponse:
                \"\"\"Create access and refresh tokens for user.\"\"\"
                access_token = create_access_token(
                    subject=user.id,
                    additional_claims={
                        "email": user.email,
                        "is_superuser": user.is_superuser,
                    },
                )
                refresh_token = create_refresh_token(subject=user.id)

                return TokenResponse(
                    access_token=access_token,
                    refresh_token=refresh_token,
                )
        """;
    }

    private String generateUserModel() {
        return """
        from datetime import datetime

        from sqlalchemy import Boolean, DateTime, String, func
        from sqlalchemy.orm import Mapped, mapped_column

        from app.models.base import Base


        class User(Base):
            \"\"\"User model for authentication.\"\"\"

            __tablename__ = "users"

            id: Mapped[int] = mapped_column(primary_key=True, index=True)
            email: Mapped[str] = mapped_column(
                String(320), unique=True, index=True, nullable=False
            )
            hashed_password: Mapped[str] = mapped_column(String(255), nullable=False)
            full_name: Mapped[str | None] = mapped_column(String(255))
            is_active: Mapped[bool] = mapped_column(Boolean, default=True)
            is_superuser: Mapped[bool] = mapped_column(Boolean, default=False)
            created_at: Mapped[datetime] = mapped_column(
                DateTime(timezone=True), server_default=func.now()
            )
            updated_at: Mapped[datetime] = mapped_column(
                DateTime(timezone=True), server_default=func.now(), onupdate=func.now()
            )

            def __repr__(self) -> str:
                return f"<User(id={self.id}, email='{self.email}')>"
        """;
    }

    private String generateUserSchemas() {
        return """
        from datetime import datetime
        from typing import Optional

        from pydantic import BaseModel, ConfigDict, EmailStr, Field


        class UserBase(BaseModel):
            \"\"\"Base user schema.\"\"\"

            email: EmailStr = Field(..., description="User email address")
            full_name: Optional[str] = Field(None, max_length=255, description="User's full name")


        class UserCreate(UserBase):
            \"\"\"Schema for creating a user.\"\"\"

            password: str = Field(..., min_length=8, max_length=100, description="User password")


        class UserUpdate(BaseModel):
            \"\"\"Schema for updating a user.\"\"\"

            email: Optional[EmailStr] = Field(None, description="User email address")
            full_name: Optional[str] = Field(None, max_length=255, description="User's full name")
            is_active: Optional[bool] = Field(None, description="Whether user is active")


        class UserResponse(UserBase):
            \"\"\"Schema for user response.\"\"\"

            model_config = ConfigDict(from_attributes=True)

            id: int = Field(..., description="User ID")
            is_active: bool = Field(..., description="Whether user is active")
            is_superuser: bool = Field(..., description="Whether user is superuser")
            created_at: datetime = Field(..., description="Creation timestamp")
            updated_at: datetime = Field(..., description="Last update timestamp")
        """;
    }

    private String generateUserRepository() {
        return """
        from typing import Optional

        from sqlalchemy import select
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.models.user import User
        from app.repositories.base_repository import BaseRepository


        class UserRepository(BaseRepository[User]):
            \"\"\"Repository for User entity operations.\"\"\"

            def __init__(self, db: AsyncSession):
                super().__init__(User, db)

            async def get_by_email(self, email: str) -> Optional[User]:
                \"\"\"
                Get user by email address.

                Args:
                    email: Email address to search for

                Returns:
                    User if found, None otherwise
                \"\"\"
                stmt = select(User).where(User.email == email)
                result = await self.db.execute(stmt)
                return result.scalar_one_or_none()

            async def get_by_id(self, user_id: int) -> Optional[User]:
                \"\"\"
                Get user by ID.

                Args:
                    user_id: User ID

                Returns:
                    User if found, None otherwise
                \"\"\"
                return await self.db.get(User, user_id)

            async def create(self, user: User) -> User:
                \"\"\"
                Create a new user.

                Args:
                    user: User to create

                Returns:
                    Created user with ID
                \"\"\"
                self.db.add(user)
                await self.db.commit()
                await self.db.refresh(user)
                return user

            async def update(self, user: User) -> User:
                \"\"\"
                Update an existing user.

                Args:
                    user: User to update

                Returns:
                    Updated user
                \"\"\"
                await self.db.commit()
                await self.db.refresh(user)
                return user
        """;
    }

    private String generateSecurityModule() {
        return """
        from passlib.context import CryptContext


        # Password hashing context using bcrypt
        pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


        def hash_password(password: str) -> str:
            \"\"\"
            Hash a password using bcrypt.

            Args:
                password: Plain text password

            Returns:
                Hashed password
            \"\"\"
            return pwd_context.hash(password)


        def verify_password(plain_password: str, hashed_password: str) -> bool:
            \"\"\"
            Verify a password against its hash.

            Args:
                plain_password: Plain text password to verify
                hashed_password: Hashed password to compare against

            Returns:
                True if password matches, False otherwise
            \"\"\"
            return pwd_context.verify(plain_password, hashed_password)
        """;
    }
}
