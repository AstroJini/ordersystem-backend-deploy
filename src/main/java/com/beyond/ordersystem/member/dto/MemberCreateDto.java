package com.beyond.ordersystem.member.dto;

import com.beyond.ordersystem.member.domain.Member;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MemberCreateDto {
    @NotEmpty(message = "이름은 필수 입력 항목입니다")
    private String name;
    @NotEmpty(message = "이메일은 필수 입력 항목입니다")
    private String email;
    @NotEmpty(message = "비밀번호는 필수 입력 항목입니다")
    @Size(min=8,max=20)
    private String password;

    public Member toEntity(String encodedPassword){
        return Member.builder()
                .name(name)
                .email(email)
                .password(encodedPassword)
                .build();
    }
}
