package com.jnzader.apigen.codegen.generator.typescript.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates TypeScript/NestJS JWT authentication functionality.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>Auth module with JWT configuration
 *   <li>Auth service for authentication logic
 *   <li>Auth controller with login/register endpoints
 *   <li>JWT strategy for passport integration
 *   <li>JWT auth guard for protecting routes
 *   <li>User entity, DTOs, and repository
 * </ul>
 */
public class TypeScriptJwtAuthGenerator {

    private static final int DEFAULT_ACCESS_TOKEN_EXPIRE_SECONDS = 1800; // 30 minutes
    private static final int DEFAULT_REFRESH_TOKEN_EXPIRE_SECONDS = 604800; // 7 days

    /**
     * Generates all JWT authentication files.
     *
     * @param accessTokenExpireSeconds access token expiration in seconds
     * @param refreshTokenExpireSeconds refresh token expiration in seconds
     * @return map of file path to content
     */
    public Map<String, String> generate(
            int accessTokenExpireSeconds, int refreshTokenExpireSeconds) {
        Map<String, String> files = new LinkedHashMap<>();

        // Auth module
        files.put("src/auth/auth.module.ts", generateAuthModule());
        files.put("src/auth/auth.service.ts", generateAuthService());
        files.put("src/auth/auth.controller.ts", generateAuthController());

        // JWT strategy and guards
        files.put("src/auth/strategies/jwt.strategy.ts", generateJwtStrategy());
        files.put("src/auth/strategies/jwt-refresh.strategy.ts", generateJwtRefreshStrategy());
        files.put("src/auth/guards/jwt-auth.guard.ts", generateJwtAuthGuard());
        files.put("src/auth/guards/jwt-refresh.guard.ts", generateJwtRefreshGuard());

        // Decorators
        files.put("src/auth/decorators/current-user.decorator.ts", generateCurrentUserDecorator());
        files.put("src/auth/decorators/public.decorator.ts", generatePublicDecorator());

        // DTOs
        files.put("src/auth/dto/login.dto.ts", generateLoginDto());
        files.put("src/auth/dto/register.dto.ts", generateRegisterDto());
        files.put("src/auth/dto/tokens.dto.ts", generateTokensDto());
        files.put("src/auth/dto/refresh-token.dto.ts", generateRefreshTokenDto());
        files.put("src/auth/dto/change-password.dto.ts", generateChangePasswordDto());

        // User module
        files.put("src/users/users.module.ts", generateUsersModule());
        files.put("src/users/entities/user.entity.ts", generateUserEntity());
        files.put("src/users/dto/user-response.dto.ts", generateUserResponseDto());
        files.put("src/users/users.service.ts", generateUsersService());

        // Index files for barrel exports
        files.put("src/auth/index.ts", generateAuthIndex());
        files.put("src/users/index.ts", generateUsersIndex());

        return files;
    }

    /** Generates with default expiration times. */
    public Map<String, String> generate() {
        return generate(DEFAULT_ACCESS_TOKEN_EXPIRE_SECONDS, DEFAULT_REFRESH_TOKEN_EXPIRE_SECONDS);
    }

    private String generateAuthModule() {
        return """
        import { Module } from '@nestjs/common';
        import { JwtModule } from '@nestjs/jwt';
        import { PassportModule } from '@nestjs/passport';
        import { ConfigModule, ConfigService } from '@nestjs/config';

        import { AuthService } from './auth.service';
        import { AuthController } from './auth.controller';
        import { JwtStrategy } from './strategies/jwt.strategy';
        import { JwtRefreshStrategy } from './strategies/jwt-refresh.strategy';
        import { UsersModule } from '../users/users.module';

        @Module({
          imports: [
            UsersModule,
            PassportModule.register({ defaultStrategy: 'jwt' }),
            JwtModule.registerAsync({
              imports: [ConfigModule],
              useFactory: async (configService: ConfigService) => ({
                secret: configService.get<string>('JWT_SECRET'),
                signOptions: {
                  expiresIn: configService.get<string>('JWT_ACCESS_EXPIRATION', '30m'),
                },
              }),
              inject: [ConfigService],
            }),
          ],
          controllers: [AuthController],
          providers: [AuthService, JwtStrategy, JwtRefreshStrategy],
          exports: [AuthService],
        })
        export class AuthModule {}
        """;
    }

    private String generateAuthService() {
        return """
        import {
          Injectable,
          UnauthorizedException,
          ConflictException,
          BadRequestException,
        } from '@nestjs/common';
        import { JwtService } from '@nestjs/jwt';
        import { ConfigService } from '@nestjs/config';
        import * as bcrypt from 'bcrypt';

        import { UsersService } from '../users/users.service';
        import { LoginDto } from './dto/login.dto';
        import { RegisterDto } from './dto/register.dto';
        import { TokensDto } from './dto/tokens.dto';
        import { User } from '../users/entities/user.entity';

        @Injectable()
        export class AuthService {
          constructor(
            private readonly usersService: UsersService,
            private readonly jwtService: JwtService,
            private readonly configService: ConfigService,
          ) {}

          async register(registerDto: RegisterDto): Promise<User> {
            const existingUser = await this.usersService.findByEmail(registerDto.email);
            if (existingUser) {
              throw new ConflictException('Email already registered');
            }

            const hashedPassword = await bcrypt.hash(registerDto.password, 10);

            return this.usersService.create({
              email: registerDto.email,
              password: hashedPassword,
              fullName: registerDto.fullName,
            });
          }

          async login(loginDto: LoginDto): Promise<TokensDto> {
            const user = await this.usersService.findByEmail(loginDto.email);

            if (!user) {
              throw new UnauthorizedException('Invalid credentials');
            }

            const isPasswordValid = await bcrypt.compare(loginDto.password, user.password);
            if (!isPasswordValid) {
              throw new UnauthorizedException('Invalid credentials');
            }

            if (!user.isActive) {
              throw new UnauthorizedException('User is inactive');
            }

            return this.generateTokens(user);
          }

          async refreshTokens(userId: number, refreshToken: string): Promise<TokensDto> {
            const user = await this.usersService.findById(userId);

            if (!user || !user.refreshToken) {
              throw new UnauthorizedException('Invalid refresh token');
            }

            const isRefreshTokenValid = await bcrypt.compare(refreshToken, user.refreshToken);
            if (!isRefreshTokenValid) {
              throw new UnauthorizedException('Invalid refresh token');
            }

            return this.generateTokens(user);
          }

          async logout(userId: number): Promise<void> {
            await this.usersService.updateRefreshToken(userId, null);
          }

          async changePassword(
            userId: number,
            currentPassword: string,
            newPassword: string,
          ): Promise<void> {
            const user = await this.usersService.findById(userId);

            if (!user) {
              throw new UnauthorizedException('User not found');
            }

            const isPasswordValid = await bcrypt.compare(currentPassword, user.password);
            if (!isPasswordValid) {
              throw new BadRequestException('Current password is incorrect');
            }

            const hashedPassword = await bcrypt.hash(newPassword, 10);
            await this.usersService.updatePassword(userId, hashedPassword);
          }

          private async generateTokens(user: User): Promise<TokensDto> {
            const payload = {
              sub: user.id,
              email: user.email,
              isAdmin: user.isAdmin,
            };

            const [accessToken, refreshToken] = await Promise.all([
              this.jwtService.signAsync(payload, {
                secret: this.configService.get<string>('JWT_SECRET'),
                expiresIn: this.configService.get<string>('JWT_ACCESS_EXPIRATION', '30m'),
              }),
              this.jwtService.signAsync(payload, {
                secret: this.configService.get<string>('JWT_REFRESH_SECRET'),
                expiresIn: this.configService.get<string>('JWT_REFRESH_EXPIRATION', '7d'),
              }),
            ]);

            // Store hashed refresh token
            const hashedRefreshToken = await bcrypt.hash(refreshToken, 10);
            await this.usersService.updateRefreshToken(user.id, hashedRefreshToken);

            return {
              accessToken,
              refreshToken,
              tokenType: 'Bearer',
            };
          }
        }
        """;
    }

    private String generateAuthController() {
        return """
        import {
          Controller,
          Post,
          Body,
          Get,
          UseGuards,
          HttpCode,
          HttpStatus,
        } from '@nestjs/common';
        import {
          ApiTags,
          ApiOperation,
          ApiResponse,
          ApiBearerAuth,
        } from '@nestjs/swagger';

        import { AuthService } from './auth.service';
        import { LoginDto } from './dto/login.dto';
        import { RegisterDto } from './dto/register.dto';
        import { TokensDto } from './dto/tokens.dto';
        import { RefreshTokenDto } from './dto/refresh-token.dto';
        import { ChangePasswordDto } from './dto/change-password.dto';
        import { JwtAuthGuard } from './guards/jwt-auth.guard';
        import { JwtRefreshGuard } from './guards/jwt-refresh.guard';
        import { CurrentUser } from './decorators/current-user.decorator';
        import { Public } from './decorators/public.decorator';
        import { UserResponseDto } from '../users/dto/user-response.dto';

        @ApiTags('Authentication')
        @Controller('auth')
        export class AuthController {
          constructor(private readonly authService: AuthService) {}

          @Post('register')
          @Public()
          @ApiOperation({ summary: 'Register a new user' })
          @ApiResponse({ status: 201, description: 'User registered successfully', type: UserResponseDto })
          @ApiResponse({ status: 409, description: 'Email already registered' })
          async register(@Body() registerDto: RegisterDto): Promise<UserResponseDto> {
            const user = await this.authService.register(registerDto);
            return new UserResponseDto(user);
          }

          @Post('login')
          @Public()
          @HttpCode(HttpStatus.OK)
          @ApiOperation({ summary: 'Login with email and password' })
          @ApiResponse({ status: 200, description: 'Login successful', type: TokensDto })
          @ApiResponse({ status: 401, description: 'Invalid credentials' })
          async login(@Body() loginDto: LoginDto): Promise<TokensDto> {
            return this.authService.login(loginDto);
          }

          @Post('refresh')
          @UseGuards(JwtRefreshGuard)
          @HttpCode(HttpStatus.OK)
          @ApiBearerAuth()
          @ApiOperation({ summary: 'Refresh access token' })
          @ApiResponse({ status: 200, description: 'Tokens refreshed', type: TokensDto })
          @ApiResponse({ status: 401, description: 'Invalid refresh token' })
          async refreshTokens(
            @CurrentUser('sub') userId: number,
            @Body() refreshTokenDto: RefreshTokenDto,
          ): Promise<TokensDto> {
            return this.authService.refreshTokens(userId, refreshTokenDto.refreshToken);
          }

          @Post('logout')
          @UseGuards(JwtAuthGuard)
          @HttpCode(HttpStatus.NO_CONTENT)
          @ApiBearerAuth()
          @ApiOperation({ summary: 'Logout current user' })
          @ApiResponse({ status: 204, description: 'Logged out successfully' })
          async logout(@CurrentUser('sub') userId: number): Promise<void> {
            await this.authService.logout(userId);
          }

          @Get('me')
          @UseGuards(JwtAuthGuard)
          @ApiBearerAuth()
          @ApiOperation({ summary: 'Get current user profile' })
          @ApiResponse({ status: 200, description: 'Current user profile', type: UserResponseDto })
          async getCurrentUser(@CurrentUser() user: any): Promise<UserResponseDto> {
            return new UserResponseDto(user);
          }

          @Post('change-password')
          @UseGuards(JwtAuthGuard)
          @HttpCode(HttpStatus.NO_CONTENT)
          @ApiBearerAuth()
          @ApiOperation({ summary: 'Change password' })
          @ApiResponse({ status: 204, description: 'Password changed successfully' })
          @ApiResponse({ status: 400, description: 'Current password is incorrect' })
          async changePassword(
            @CurrentUser('sub') userId: number,
            @Body() changePasswordDto: ChangePasswordDto,
          ): Promise<void> {
            await this.authService.changePassword(
              userId,
              changePasswordDto.currentPassword,
              changePasswordDto.newPassword,
            );
          }
        }
        """;
    }

    private String generateJwtStrategy() {
        return """
        import { Injectable, UnauthorizedException } from '@nestjs/common';
        import { PassportStrategy } from '@nestjs/passport';
        import { ExtractJwt, Strategy } from 'passport-jwt';
        import { ConfigService } from '@nestjs/config';

        import { UsersService } from '../../users/users.service';

        @Injectable()
        export class JwtStrategy extends PassportStrategy(Strategy, 'jwt') {
          constructor(
            private readonly configService: ConfigService,
            private readonly usersService: UsersService,
          ) {
            super({
              jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
              ignoreExpiration: false,
              secretOrKey: configService.get<string>('JWT_SECRET'),
            });
          }

          async validate(payload: any) {
            const user = await this.usersService.findById(payload.sub);

            if (!user || !user.isActive) {
              throw new UnauthorizedException('User not found or inactive');
            }

            return {
              sub: user.id,
              email: user.email,
              isAdmin: user.isAdmin,
            };
          }
        }
        """;
    }

    private String generateJwtRefreshStrategy() {
        return """
        import { Injectable } from '@nestjs/common';
        import { PassportStrategy } from '@nestjs/passport';
        import { ExtractJwt, Strategy } from 'passport-jwt';
        import { ConfigService } from '@nestjs/config';

        @Injectable()
        export class JwtRefreshStrategy extends PassportStrategy(Strategy, 'jwt-refresh') {
          constructor(private readonly configService: ConfigService) {
            super({
              jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
              ignoreExpiration: false,
              secretOrKey: configService.get<string>('JWT_REFRESH_SECRET'),
            });
          }

          async validate(payload: any) {
            return {
              sub: payload.sub,
              email: payload.email,
              isAdmin: payload.isAdmin,
            };
          }
        }
        """;
    }

    private String generateJwtAuthGuard() {
        return """
        import { ExecutionContext, Injectable } from '@nestjs/common';
        import { Reflector } from '@nestjs/core';
        import { AuthGuard } from '@nestjs/passport';

        import { IS_PUBLIC_KEY } from '../decorators/public.decorator';

        @Injectable()
        export class JwtAuthGuard extends AuthGuard('jwt') {
          constructor(private reflector: Reflector) {
            super();
          }

          canActivate(context: ExecutionContext) {
            const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
              context.getHandler(),
              context.getClass(),
            ]);

            if (isPublic) {
              return true;
            }

            return super.canActivate(context);
          }
        }
        """;
    }

    private String generateJwtRefreshGuard() {
        return """
        import { Injectable } from '@nestjs/common';
        import { AuthGuard } from '@nestjs/passport';

        @Injectable()
        export class JwtRefreshGuard extends AuthGuard('jwt-refresh') {}
        """;
    }

    private String generateCurrentUserDecorator() {
        return """
        import { createParamDecorator, ExecutionContext } from '@nestjs/common';

        export const CurrentUser = createParamDecorator(
          (data: string | undefined, ctx: ExecutionContext) => {
            const request = ctx.switchToHttp().getRequest();
            const user = request.user;

            return data ? user?.[data] : user;
          },
        );
        """;
    }

    private String generatePublicDecorator() {
        return """
        import { SetMetadata } from '@nestjs/common';

        export const IS_PUBLIC_KEY = 'isPublic';
        export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
        """;
    }

    private String generateLoginDto() {
        return """
        import { ApiProperty } from '@nestjs/swagger';
        import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';

        export class LoginDto {
          @ApiProperty({ example: 'user@example.com', description: 'User email address' })
          @IsEmail()
          @IsNotEmpty()
          email: string;

          @ApiProperty({ example: 'password123', description: 'User password (min 8 chars)' })
          @IsString()
          @IsNotEmpty()
          @MinLength(8)
          password: string;
        }
        """;
    }

    private String generateRegisterDto() {
        return """
        import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
        import { IsEmail, IsNotEmpty, IsOptional, IsString, MaxLength, MinLength } from 'class-validator';

        export class RegisterDto {
          @ApiProperty({ example: 'user@example.com', description: 'User email address' })
          @IsEmail()
          @IsNotEmpty()
          email: string;

          @ApiProperty({ example: 'password123', description: 'User password (min 8 chars)' })
          @IsString()
          @IsNotEmpty()
          @MinLength(8)
          @MaxLength(100)
          password: string;

          @ApiPropertyOptional({ example: 'John Doe', description: 'User full name' })
          @IsString()
          @IsOptional()
          @MaxLength(255)
          fullName?: string;
        }
        """;
    }

    private String generateTokensDto() {
        return """
        import { ApiProperty } from '@nestjs/swagger';

        export class TokensDto {
          @ApiProperty({ description: 'JWT access token' })
          accessToken: string;

          @ApiProperty({ description: 'JWT refresh token' })
          refreshToken: string;

          @ApiProperty({ example: 'Bearer', description: 'Token type' })
          tokenType: string;
        }
        """;
    }

    private String generateRefreshTokenDto() {
        return """
        import { ApiProperty } from '@nestjs/swagger';
        import { IsNotEmpty, IsString } from 'class-validator';

        export class RefreshTokenDto {
          @ApiProperty({ description: 'Refresh token' })
          @IsString()
          @IsNotEmpty()
          refreshToken: string;
        }
        """;
    }

    private String generateChangePasswordDto() {
        return """
        import { ApiProperty } from '@nestjs/swagger';
        import { IsNotEmpty, IsString, MaxLength, MinLength } from 'class-validator';

        export class ChangePasswordDto {
          @ApiProperty({ description: 'Current password' })
          @IsString()
          @IsNotEmpty()
          currentPassword: string;

          @ApiProperty({ description: 'New password (min 8 chars)' })
          @IsString()
          @IsNotEmpty()
          @MinLength(8)
          @MaxLength(100)
          newPassword: string;
        }
        """;
    }

    private String generateUsersModule() {
        return """
        import { Module } from '@nestjs/common';
        import { TypeOrmModule } from '@nestjs/typeorm';

        import { User } from './entities/user.entity';
        import { UsersService } from './users.service';

        @Module({
          imports: [TypeOrmModule.forFeature([User])],
          providers: [UsersService],
          exports: [UsersService],
        })
        export class UsersModule {}
        """;
    }

    private String generateUserEntity() {
        return """
        import {
          Entity,
          PrimaryGeneratedColumn,
          Column,
          CreateDateColumn,
          UpdateDateColumn,
        } from 'typeorm';

        @Entity('users')
        export class User {
          @PrimaryGeneratedColumn()
          id: number;

          @Column({ unique: true, length: 320 })
          email: string;

          @Column({ length: 255 })
          password: string;

          @Column({ name: 'full_name', length: 255, nullable: true })
          fullName?: string;

          @Column({ name: 'is_active', default: true })
          isActive: boolean;

          @Column({ name: 'is_admin', default: false })
          isAdmin: boolean;

          @Column({ name: 'refresh_token', length: 255, nullable: true })
          refreshToken?: string;

          @CreateDateColumn({ name: 'created_at' })
          createdAt: Date;

          @UpdateDateColumn({ name: 'updated_at' })
          updatedAt: Date;
        }
        """;
    }

    private String generateUserResponseDto() {
        return """
        import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
        import { Exclude, Expose } from 'class-transformer';

        import { User } from '../entities/user.entity';

        export class UserResponseDto {
          @ApiProperty({ description: 'User ID' })
          @Expose()
          id: number;

          @ApiProperty({ description: 'User email' })
          @Expose()
          email: string;

          @ApiPropertyOptional({ description: 'User full name' })
          @Expose()
          fullName?: string;

          @ApiProperty({ description: 'Whether user is active' })
          @Expose()
          isActive: boolean;

          @ApiProperty({ description: 'Whether user is admin' })
          @Expose()
          isAdmin: boolean;

          @ApiProperty({ description: 'Creation timestamp' })
          @Expose()
          createdAt: Date;

          @Exclude()
          password: string;

          @Exclude()
          refreshToken?: string;

          constructor(user: User) {
            Object.assign(this, user);
          }
        }
        """;
    }

    private String generateUsersService() {
        return """
        import { Injectable } from '@nestjs/common';
        import { InjectRepository } from '@nestjs/typeorm';
        import { Repository } from 'typeorm';

        import { User } from './entities/user.entity';

        @Injectable()
        export class UsersService {
          constructor(
            @InjectRepository(User)
            private readonly userRepository: Repository<User>,
          ) {}

          async create(data: Partial<User>): Promise<User> {
            const user = this.userRepository.create(data);
            return this.userRepository.save(user);
          }

          async findById(id: number): Promise<User | null> {
            return this.userRepository.findOne({ where: { id } });
          }

          async findByEmail(email: string): Promise<User | null> {
            return this.userRepository.findOne({ where: { email } });
          }

          async updateRefreshToken(id: number, refreshToken: string | null): Promise<void> {
            await this.userRepository.update(id, { refreshToken });
          }

          async updatePassword(id: number, password: string): Promise<void> {
            await this.userRepository.update(id, { password });
          }
        }
        """;
    }

    private String generateAuthIndex() {
        return """
        export * from './auth.module';
        export * from './auth.service';
        export * from './auth.controller';
        export * from './guards/jwt-auth.guard';
        export * from './guards/jwt-refresh.guard';
        export * from './decorators/current-user.decorator';
        export * from './decorators/public.decorator';
        export * from './dto/login.dto';
        export * from './dto/register.dto';
        export * from './dto/tokens.dto';
        export * from './dto/refresh-token.dto';
        export * from './dto/change-password.dto';
        """;
    }

    private String generateUsersIndex() {
        return """
        export * from './users.module';
        export * from './users.service';
        export * from './entities/user.entity';
        export * from './dto/user-response.dto';
        """;
    }
}
