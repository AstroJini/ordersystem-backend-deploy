package com.beyond.ordersystem.member.controller;

import com.beyond.ordersystem.common.auth.JwtTokenProvider;
import com.beyond.ordersystem.common.dto.CommonDto;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.*;
import com.beyond.ordersystem.member.service.MemberService;
import com.beyond.ordersystem.product.dto.ProductResDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;
    @PostMapping("/create")
    public ResponseEntity<?> save(@RequestBody @Valid MemberCreateDto memberCreateDto) {
        Long id = memberService.save(memberCreateDto);
        return new  ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원가입완료")
                        .build()
                , HttpStatus.CREATED);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> doLogin(@RequestBody LoginReqDto  loginReqDto) {
        Member member = memberService.doLogin(loginReqDto);
//        at 토큰생성
        String accessToken = jwtTokenProvider.createAtToken(member);
//        rt 톤큰 생성
        String refreshToken = jwtTokenProvider.createRtToken(member);

        LoginResDto loginResDto = LoginResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(loginResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("로그인성공")
                        .build()
                , HttpStatus.OK);
    }
//        rt 를 통한 at 갱신요청
    @PostMapping("/refresh-at")
    public ResponseEntity<?> generateNewAt(@RequestBody RefreshTokenDto refreshTokenDto) {
//        rt 검증로직
        Member member = jwtTokenProvider.validateRt(refreshTokenDto.getRefreshToken());
//        at 신규생성
        String accessToken = jwtTokenProvider.createAtToken(member);
        LoginResDto loginResDto = LoginResDto.builder()
                .accessToken(accessToken)
                .build();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(loginResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("at 재발급 성공")
                        .build()
                , HttpStatus.OK);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll(){
        List<MemberResDto> memberResDtos =  memberService.findAll();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(memberResDtos)
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원목록조회완료")
                        .build(), HttpStatus.OK);
    }
    @GetMapping("/myinfo")
    public ResponseEntity<?> getMyInfo(){
        return new  ResponseEntity<>(
                CommonDto.builder()
                        .result(memberService.myinfo())
                        .status_message("내정보조회")
                        .status_code(HttpStatus.OK.value())
                        .build(), HttpStatus.OK);
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        MemberResDto memberResDto = memberService.findById(id);
        return new  ResponseEntity<>(
                CommonDto.builder()
                        .result(memberResDto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("멤버상세조회완료")
                        .build()
                , HttpStatus.OK);

    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(){
        memberService.delete();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result("OK")
                        .status_code(HttpStatus.OK.value())
                        .status_message("회원탈퇴완료")
                        .build(), HttpStatus.OK);
    }
}
