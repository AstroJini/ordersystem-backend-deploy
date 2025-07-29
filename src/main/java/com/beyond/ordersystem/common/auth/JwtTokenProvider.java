package com.beyond.ordersystem.common.auth;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expirationAt}")
    private long expirationAt;

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    @Value("${jwt.expirationRt}")
    private long expirationRt;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    private Key secret_at_key;
    private Key secret_rt_key;

//    Qualifier는 기본적으로 메서드를 통한 주입 가능, 그래서 , 이 경우 생성자 주입방식을 해야 Qualifier사용 가능
    public JwtTokenProvider(MemberRepository memberRepository, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate) {
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init(){
        secret_at_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyAt), SignatureAlgorithm.HS256.getJcaName());
        secret_rt_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyRt), SignatureAlgorithm.HS256.getJcaName());
    }
    public String createAtToken(Member member){
        String email = member.getEmail();
        String role = member.getRole().toString();
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);
        Date now = new Date();
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationAt*60*1000))
                .signWith(secret_at_key)
                .compact();
        return accessToken;
    }

    public String createRtToken(Member member){
//        유효기간이 긴 rt 토큰 생성
        String email = member.getEmail();
        String role = member.getRole().toString();
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);
        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt*60*1000))
                .signWith(secret_rt_key)
                .compact();

//        rt 토큰을 redis에 저장 : key-value 형식으로 set
        redisTemplate.opsForValue().set(member.getEmail(), refreshToken);
//        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 200, TimeUnit.DAYS); //200일 ttl(time to live: 유효기간)
        return refreshToken;
    }

    public Member validateRt(String refreshToken){
//       rt 그 자체를 검증
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKeyRt)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();
        String email  = claims.getSubject();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("email"));
//        redis의 값과 비교하는 검증
        String redisRt = redisTemplate.opsForValue().get(member.getEmail());
        if(!redisRt.equals(refreshToken)){
            throw new IllegalStateException("잘못된 토큰입니다");
        }
        return member;
    }
}
