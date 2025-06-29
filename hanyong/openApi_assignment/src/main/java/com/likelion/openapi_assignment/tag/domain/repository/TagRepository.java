package com.likelion.openapi_assignment.tag.domain.repository;

import com.likelion.openapi_assignment.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);
}
