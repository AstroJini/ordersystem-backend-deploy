package com.beyond.ordersystem.common.service;

import com.beyond.ordersystem.common.dto.SseMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SseAlarmService implements MessageListener {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final RedisTemplate<String, String> redisTemplate;

    public SseAlarmService(SseEmitterRegistry sseEmitterRegistry, @Qualifier("ssePubSub")RedisTemplate<String, String> redisTemplate) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.redisTemplate = redisTemplate;
    }

    //    특정사용자에게 message 발송
//    Long productId 나중에 커스텀
    public void publishMessage(String receiver, String sender, Long orderingId){
        SseMessageDto sseMessageDto = SseMessageDto.builder()
                .orderingId(orderingId)
                .sender(sender)
                .receiver(receiver)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String data = null;
        try {
            data = objectMapper.writeValueAsString(sseMessageDto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
//        emitter 객체를 통해 메시지 전송
        SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(receiver);
//        emitter 객체가 현재 서버에 있으면, 직접 알림 발송. 그렇지 않으면, redis에 publish
        if(sseEmitter != null){
            try {
                sseEmitter.send(SseEmitter.event().name("ordered").data(data));
//                사용자가 로그아웃(새로고침) 후에 다시 화면에 들어왔을때 알림메시지가 남아있으려면 DB에 추가적으로 저장필요
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            redisTemplate.convertAndSend("order-channel", data);
        }

    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
//        Message : 실질적인 메시지가 담겨있는 객체
//        pattern : 채널명
        String channel_name = new String(pattern);
//        여러개의 채널을 구독하고 있을경우, 채널명으로 분기처리
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SseMessageDto sseMessageDto = objectMapper.readValue(message.getBody(), SseMessageDto.class);
            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(sseMessageDto.getReceiver());
//        emitter 객체가 현재 서버에 있으면, 직접 알림 발송. 그렇지 않으면, redis에 publish
            if(sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().name("ordered").data(sseMessageDto));
//                사용자가 로그아웃(새로고침) 후에 다시 화면에 들어왔을때 알림메시지가 남아있으려면 DB에 추가적으로 저장필요
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(sseMessageDto);
            System.out.println(channel_name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
