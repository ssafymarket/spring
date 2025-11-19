package org.ssafy.ssafymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssafy.ssafymarket.dto.ChatRoomDto;
import org.ssafy.ssafymarket.entity.ChatRoom;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.User;
import org.ssafy.ssafymarket.repository.ChatRoomRepository;
import org.ssafy.ssafymarket.repository.PostRepository;
import org.ssafy.ssafymarket.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    /**
     * 채팅방 생성 또는 기존 채팅방 반환
     */
    @Transactional
    public ChatRoomDto createOrGetChatRoom(String buyerId, Long postId) {
        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));

        String sellerId = post.getWriter().getStudentId();

        // 본인 게시글에는 채팅 불가
        if (buyerId.equals(sellerId)) {
            throw new IllegalArgumentException("본인 게시글에는 채팅할 수 없습니다");
        }

        // 기존 채팅방 확인
        ChatRoom chatRoom = chatRoomRepository.findByPost_PostIdAndBuyer_StudentId(postId, buyerId)
			.map(room -> {
				if(room.getActivity()==0){
					room.setActivity(1);
				}
				return room;
			})
			.orElseGet(() -> {
                    // 새 채팅방 생성
                    User buyer = userRepository.findByStudentId(buyerId)
                            .orElseThrow(() -> new IllegalArgumentException("구매자를 찾을 수 없습니다"));

                    ChatRoom newRoom = ChatRoom.builder()
                            .post(post)
                            .buyer(buyer)
                            .seller(post.getWriter())
                            .build();

                    ChatRoom savedRoom = chatRoomRepository.save(newRoom);

                    // 게시글의 채팅방 개수 증가
                    post.setChatRoomCount(post.getChatRoomCount() + 1);
                    postRepository.save(post);

                    log.info("새 채팅방 생성 - roomId: {}, postId: {}, buyer: {}, seller: {}",
                            savedRoom.getRoomId(), postId, buyerId, sellerId);

                    return savedRoom;
                });

        return ChatRoomDto.fromEntity(chatRoom, buyerId);
    }

    /**
     * 사용자의 모든 채팅방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(String userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByUserId(userId);

        return chatRooms.stream()
                .map(chatRoom -> ChatRoomDto.fromEntity(chatRoom, userId))
                .collect(Collectors.toList());
    }

    /**
     * 특정 채팅방 조회
     */
    @Transactional(readOnly = true)
    public ChatRoomDto getChatRoom(Long roomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        // 권한 확인 (구매자 또는 판매자만 접근 가능)
        if (!chatRoom.getBuyer().getStudentId().equals(userId) &&
            !chatRoom.getSeller().getStudentId().equals(userId)) {
            throw new IllegalArgumentException("채팅방에 접근할 권한이 없습니다");
        }

        return ChatRoomDto.fromEntity(chatRoom, userId);
    }

    /**
     * 채팅방 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsChatRoom(Long roomId) {
        return chatRoomRepository.existsById(roomId);
    }

    /**
     * 채팅방 나가기
     * - 채팅방을 비활성화(soft delete)하고 게시글의 채팅방 개수를 감소시킴
     */
    @Transactional
    public void leaveChatRoom(Long roomId, String userId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        // 권한 확인 (구매자 또는 판매자만 나갈 수 있음)
        if (!chatRoom.getBuyer().getStudentId().equals(userId) &&
            !chatRoom.getSeller().getStudentId().equals(userId)) {
            throw new IllegalArgumentException("채팅방에 접근할 권한이 없습니다");
        }

        // 게시글의 채팅방 개수 감소
        Post post = chatRoom.getPost();
        if (post.getChatRoomCount() > 0) {
            post.setChatRoomCount(post.getChatRoomCount() - 1);
            postRepository.save(post);
        }

        // 채팅방 비활성화 (soft delete)
        chatRoom.setActivity(0);
        chatRoomRepository.save(chatRoom);

        log.info("채팅방 나가기 완료 - roomId: {}, userId: {}, postId: {}",
                roomId, userId, post.getPostId());
    }
}
