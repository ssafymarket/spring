package org.ssafy.ssafymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.ssafy.ssafymarket.entity.Post;
import org.ssafy.ssafymarket.entity.User;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByWriter(User writer);
    List<Post> findByStatus(Post.PostStatus status);

    @Query("SELECT p FROM Post p WHERE p.writer.studentId = :studentId ORDER BY p.createdAt DESC")
    List<Post> findByWriterStudentIdOrderByCreatedAtDesc(@Param("studentId") String studentId);
}
