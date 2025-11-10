package org.ssafy.ssafymarket.dto;

import lombok.*;
import org.ssafy.ssafymarket.entity.ChatMessage;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {
    private String content;
    private ChatMessage.MessageType messageType;
}
