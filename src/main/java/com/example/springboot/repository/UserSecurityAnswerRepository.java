package com.example.springboot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot.model.UserSecurityAnswer;

@Repository
public interface UserSecurityAnswerRepository extends JpaRepository<UserSecurityAnswer, Integer> {

    long countByUserUserId(Integer userId);

    List<UserSecurityAnswer> findByUserUserIdOrderBySlot(Integer userId);

    @Transactional
    void deleteByUserUserId(Integer userId);
}
