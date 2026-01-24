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
package com.jnzader.apigen.codegen.generator.typescript.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset flow for TypeScript/NestJS applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class TypeScriptPasswordResetGenerator {

    /**
     * Generates password reset files.
     *
     * @param tokenExpirationMinutes token expiration in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int tokenExpirationMinutes) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("src/auth/password-reset/password-reset.module.ts", generateModule());
        files.put(
                "src/auth/password-reset/password-reset.service.ts",
                generateService(tokenExpirationMinutes));
        files.put("src/auth/password-reset/password-reset.controller.ts", generateController());
        files.put("src/auth/password-reset/dto/password-reset.dto.ts", generateDto());
        files.put(
                "src/auth/password-reset/entities/password-reset-token.entity.ts",
                generateEntity());

        return files;
    }

    private String generateModule() {
        return """
        import { Module } from '@nestjs/common';
        import { TypeOrmModule } from '@nestjs/typeorm';
        import { PasswordResetService } from './password-reset.service';
        import { PasswordResetController } from './password-reset.controller';
        import { PasswordResetToken } from './entities/password-reset-token.entity';
        import { MailModule } from '../../mail/mail.module';
        import { UsersModule } from '../../users/users.module';

        @Module({
          imports: [
            TypeOrmModule.forFeature([PasswordResetToken]),
            MailModule,
            UsersModule,
          ],
          controllers: [PasswordResetController],
          providers: [PasswordResetService],
          exports: [PasswordResetService],
        })
        export class PasswordResetModule {}
        """;
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                import { Injectable, Logger, BadRequestException } from '@nestjs/common';
                import { InjectRepository } from '@nestjs/typeorm';
                import { Repository, LessThan } from 'typeorm';
                import { randomBytes } from 'crypto';
                import { PasswordResetToken } from './entities/password-reset-token.entity';
                import { MailService } from '../../mail/mail.service';
                import { UsersService } from '../../users/users.service';
                import * as bcrypt from 'bcrypt';

                @Injectable()
                export class PasswordResetService {
                  private readonly logger = new Logger(PasswordResetService.name);
                  private readonly TOKEN_EXPIRATION_MINUTES = %d;

                  constructor(
                    @InjectRepository(PasswordResetToken)
                    private readonly tokenRepository: Repository<PasswordResetToken>,
                    private readonly mailService: MailService,
                    private readonly usersService: UsersService,
                  ) {}

                  async requestPasswordReset(email: string, baseUrl: string): Promise<boolean> {
                    const user = await this.usersService.findByEmail(email);

                    if (!user) {
                      // Return true to prevent email enumeration
                      return true;
                    }

                    // Invalidate existing tokens
                    await this.invalidateExistingTokens(user.id);

                    // Generate new token
                    const token = randomBytes(32).toString('hex');
                    const expiresAt = new Date();
                    expiresAt.setMinutes(expiresAt.getMinutes() + this.TOKEN_EXPIRATION_MINUTES);

                    const resetToken = this.tokenRepository.create({
                      userId: user.id,
                      token,
                      expiresAt,
                    });

                    await this.tokenRepository.save(resetToken);

                    // Send email
                    const resetLink = `${baseUrl}/reset-password?token=${token}`;
                    await this.mailService.sendPasswordResetEmail(
                      email,
                      user.username || user.email,
                      resetLink,
                      this.TOKEN_EXPIRATION_MINUTES,
                    );

                    this.logger.log(`Password reset email sent to: ${email}`);
                    return true;
                  }

                  async validateToken(token: string): Promise<{ valid: boolean; message?: string }> {
                    const resetToken = await this.tokenRepository.findOne({
                      where: { token },
                    });

                    if (!resetToken) {
                      return { valid: false, message: 'Invalid or expired token' };
                    }

                    if (resetToken.used) {
                      return { valid: false, message: 'Token has already been used' };
                    }

                    if (resetToken.expiresAt < new Date()) {
                      return { valid: false, message: 'Token has expired' };
                    }

                    return { valid: true };
                  }

                  async resetPassword(token: string, newPassword: string): Promise<{ success: boolean; message: string }> {
                    const validation = await this.validateToken(token);
                    if (!validation.valid) {
                      throw new BadRequestException(validation.message);
                    }

                    const resetToken = await this.tokenRepository.findOne({
                      where: { token },
                    });

                    const user = await this.usersService.findById(resetToken.userId);
                    if (!user) {
                      throw new BadRequestException('User not found');
                    }

                    // Update password
                    const hashedPassword = await bcrypt.hash(newPassword, 10);
                    await this.usersService.updatePassword(user.id, hashedPassword);

                    // Mark token as used
                    resetToken.used = true;
                    await this.tokenRepository.save(resetToken);

                    this.logger.log(`Password reset for user: ${user.email}`);
                    return { success: true, message: 'Password has been reset successfully' };
                  }

                  private async invalidateExistingTokens(userId: string): Promise<void> {
                    await this.tokenRepository.update(
                      { userId, used: false },
                      { used: true },
                    );
                  }

                  async cleanupExpiredTokens(): Promise<void> {
                    const result = await this.tokenRepository.delete({
                      expiresAt: LessThan(new Date()),
                    });
                    this.logger.log(`Cleaned up ${result.affected} expired tokens`);
                  }
                }
                """,
                tokenExpirationMinutes);
    }

    private String generateController() {
        return """
        import { Controller, Post, Body, HttpCode, HttpStatus, Req } from '@nestjs/common';
        import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
        import { Request } from 'express';
        import { PasswordResetService } from './password-reset.service';
        import {
          ForgotPasswordDto,
          ResetPasswordDto,
          ValidateTokenDto,
          ForgotPasswordResponseDto,
          ResetPasswordResponseDto,
          ValidateTokenResponseDto,
        } from './dto/password-reset.dto';

        @ApiTags('Password Reset')
        @Controller('auth/password')
        export class PasswordResetController {
          constructor(private readonly passwordResetService: PasswordResetService) {}

          @Post('forgot')
          @HttpCode(HttpStatus.OK)
          @ApiOperation({ summary: 'Request password reset email' })
          @ApiResponse({ status: 200, type: ForgotPasswordResponseDto })
          async forgotPassword(
            @Req() req: Request,
            @Body() dto: ForgotPasswordDto,
          ): Promise<ForgotPasswordResponseDto> {
            const baseUrl = `${req.protocol}://${req.get('host')}`;
            await this.passwordResetService.requestPasswordReset(dto.email, baseUrl);
            return {
              message: 'If the email exists, a password reset link has been sent.',
            };
          }

          @Post('validate')
          @HttpCode(HttpStatus.OK)
          @ApiOperation({ summary: 'Validate reset token' })
          @ApiResponse({ status: 200, type: ValidateTokenResponseDto })
          async validateToken(
            @Body() dto: ValidateTokenDto,
          ): Promise<ValidateTokenResponseDto> {
            return this.passwordResetService.validateToken(dto.token);
          }

          @Post('reset')
          @HttpCode(HttpStatus.OK)
          @ApiOperation({ summary: 'Reset password with token' })
          @ApiResponse({ status: 200, type: ResetPasswordResponseDto })
          async resetPassword(
            @Body() dto: ResetPasswordDto,
          ): Promise<ResetPasswordResponseDto> {
            if (dto.newPassword !== dto.confirmPassword) {
              throw new Error('Passwords do not match');
            }
            return this.passwordResetService.resetPassword(dto.token, dto.newPassword);
          }
        }
        """;
    }

    private String generateDto() {
        return """
        import { IsEmail, IsNotEmpty, IsString, MinLength, MaxLength } from 'class-validator';
        import { ApiProperty } from '@nestjs/swagger';

        export class ForgotPasswordDto {
          @ApiProperty({ description: 'User email address' })
          @IsEmail()
          @IsNotEmpty()
          email: string;
        }

        export class ValidateTokenDto {
          @ApiProperty({ description: 'Password reset token' })
          @IsString()
          @IsNotEmpty()
          token: string;
        }

        export class ResetPasswordDto {
          @ApiProperty({ description: 'Password reset token' })
          @IsString()
          @IsNotEmpty()
          token: string;

          @ApiProperty({ description: 'New password' })
          @IsString()
          @MinLength(8)
          @MaxLength(128)
          newPassword: string;

          @ApiProperty({ description: 'Confirm new password' })
          @IsString()
          @MinLength(8)
          @MaxLength(128)
          confirmPassword: string;
        }

        export class ForgotPasswordResponseDto {
          @ApiProperty()
          message: string;
        }

        export class ValidateTokenResponseDto {
          @ApiProperty()
          valid: boolean;

          @ApiProperty({ required: false })
          message?: string;
        }

        export class ResetPasswordResponseDto {
          @ApiProperty()
          success: boolean;

          @ApiProperty()
          message: string;
        }
        """;
    }

    private String generateEntity() {
        return """
        import {
          Entity,
          Column,
          PrimaryGeneratedColumn,
          CreateDateColumn,
          ManyToOne,
          JoinColumn,
        } from 'typeorm';
        import { User } from '../../../users/entities/user.entity';

        @Entity('password_reset_tokens')
        export class PasswordResetToken {
          @PrimaryGeneratedColumn('uuid')
          id: string;

          @Column({ name: 'user_id' })
          userId: string;

          @Column({ unique: true })
          token: string;

          @Column({ name: 'expires_at' })
          expiresAt: Date;

          @Column({ default: false })
          used: boolean;

          @CreateDateColumn({ name: 'created_at' })
          createdAt: Date;

          @ManyToOne(() => User)
          @JoinColumn({ name: 'user_id' })
          user: User;
        }
        """;
    }
}
