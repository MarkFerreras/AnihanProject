package com.example.springboot.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One of a user's 2 "forgot password" question+answer slots.
 *
 * <p>Exactly one of {@link #question} / {@link #customQuestion} is set per
 * row, never both and never neither — enforced by the DB
 * {@code chk_question_xor_custom} check constraint, and validated again at
 * the service layer before save. {@link #customQuestion} is stored in
 * plaintext (it is not the secret; the answer is). {@link #answerHash} is
 * BCrypt, hashed from an already-trimmed, lowercased answer so comparison
 * is case-insensitive.
 */
@Entity
@Table(name = "user_security_answers")
public class UserSecurityAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Integer answerId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private SecurityQuestion question;

    @Column(name = "custom_question")
    private String customQuestion;

    @Column(name = "answer_hash", nullable = false)
    private String answerHash;

    @Column(name = "slot", nullable = false)
    private Integer slot;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SecurityQuestion getQuestion() {
        return question;
    }

    public void setQuestion(SecurityQuestion question) {
        this.question = question;
    }

    public String getCustomQuestion() {
        return customQuestion;
    }

    public void setCustomQuestion(String customQuestion) {
        this.customQuestion = customQuestion;
    }

    public String getAnswerHash() {
        return answerHash;
    }

    public void setAnswerHash(String answerHash) {
        this.answerHash = answerHash;
    }

    public Integer getSlot() {
        return slot;
    }

    public void setSlot(Integer slot) {
        this.slot = slot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** The question text to display, regardless of whether it's a default or custom one. */
    public String resolveQuestionText() {
        return question != null ? question.getQuestionText() : customQuestion;
    }
}
