package com.ssafy.banana.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssafy.banana.db.entity.ArtCategory;

public interface ArtCategoryRepository extends JpaRepository<ArtCategory, Long> {

}
