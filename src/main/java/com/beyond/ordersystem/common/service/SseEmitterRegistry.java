package com.beyond.ordersystem.common.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

@Component
public class SseEmitterRegistry {

    //    SseEmitter 는 연결된 사용자 정보(ip, macaddress 정보 등 ....)를 의미
    private Map<String, SseEmitter> emitterMap = new HashMap();

    //        사용자가 로그아웃후에 다시 화면에 들어왔을때 알림메시지가 남아있으려면 DB에 추가적으로 저장 필요
    public void addSseEmitter(String email , SseEmitter sseEmitter){
        emitterMap.put(email, sseEmitter);
        System.out.println(emitterMap);
    }

    public void removeSseEmitter(String email){
        emitterMap.remove(email);
        System.out.println(emitterMap);
    }

    public SseEmitter getEmitter(String email){
        return emitterMap.get(email);
    }
}
