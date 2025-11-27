package org.ssafy.ssafymarket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<Post> findByBuyer(User buyer);

    Page<Post> findByStatus(Post.PostStatus status, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.category = :category")
    Page<Post> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.writer.studentId = :studentId ORDER BY p.createdAt DESC")
    List<Post> findByWriterStudentIdOrderByCreatedAtDesc(@Param("studentId") String studentId);

    // 인기순 정렬 (좋아요 수 기준)
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> findAllByPopularity(Pageable pageable);

    // 카테고리별 인기순 정렬
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "WHERE p.category = :category " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> findByCategoryByPopularity(@Param("category") String category, Pageable pageable);

    // 검색 인기순 정렬
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> searchByKeywordByPopularity(@Param("keyword") String keyword, Pageable pageable);

    // 검색 + 상태별 인기순 정렬
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> searchByKeywordAndStatusByPopularity(@Param("keyword") String keyword,
                                                     @Param("status") Post.PostStatus status,
                                                     Pageable pageable);

    // 전체 검색 (status 필터 없음)
    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // status별 검색 (판매중/판매완료 선택 가능)
    @Query("SELECT p FROM Post p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status")
    Page<Post> searchByKeywordAndStatus(@Param("keyword") String keyword,
                                         @Param("status") Post.PostStatus status,
                                         Pageable pageable);

    // === 캠퍼스별 필터링 쿼리 ===

    // 캠퍼스별 전체 게시글 조회
    @Query("SELECT p FROM Post p WHERE p.writer.campus = :campus")
    Page<Post> findByCampus(@Param("campus") User.Campus campus, Pageable pageable);

    // 캠퍼스별 인기순 정렬
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "WHERE p.writer.campus = :campus " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> findByCampusByPopularity(@Param("campus") User.Campus campus, Pageable pageable);

    // 캠퍼스 + 카테고리별 조회
    @Query("SELECT p FROM Post p WHERE p.writer.campus = :campus AND p.category = :category")
    Page<Post> findByCampusAndCategory(@Param("campus") User.Campus campus,
                                        @Param("category") String category,
                                        Pageable pageable);

    // 캠퍼스 + 카테고리별 인기순
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "WHERE p.writer.campus = :campus AND p.category = :category " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> findByCampusAndCategoryByPopularity(@Param("campus") User.Campus campus,
                                                     @Param("category") String category,
                                                     Pageable pageable);

    // 캠퍼스 + 검색
    @Query("SELECT p FROM Post p WHERE " +
           "p.writer.campus = :campus AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchByCampusAndKeyword(@Param("campus") User.Campus campus,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    // 캠퍼스 + 검색 인기순
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "WHERE p.writer.campus = :campus AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> searchByCampusAndKeywordByPopularity(@Param("campus") User.Campus campus,
                                                     @Param("keyword") String keyword,
                                                     Pageable pageable);

    // 캠퍼스 + 검색 + 상태별
    @Query("SELECT p FROM Post p WHERE " +
           "p.writer.campus = :campus AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status")
    Page<Post> searchByCampusAndKeywordAndStatus(@Param("campus") User.Campus campus,
                                                  @Param("keyword") String keyword,
                                                  @Param("status") Post.PostStatus status,
                                                  Pageable pageable);

    // 캠퍼스 + 검색 + 상태별 인기순
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN PostLike pl ON pl.postId = p.postId " +
           "WHERE p.writer.campus = :campus AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "p.status = :status " +
           "GROUP BY p.postId " +
           "ORDER BY COUNT(pl) DESC, p.createdAt DESC")
    Page<Post> searchByCampusAndKeywordAndStatusByPopularity(@Param("campus") User.Campus campus,
                                                              @Param("keyword") String keyword,
                                                              @Param("status") Post.PostStatus status,
                                                              Pageable pageable);
}
