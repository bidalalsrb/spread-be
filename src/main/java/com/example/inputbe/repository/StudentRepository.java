package com.example.inputbe.repository;

import com.example.inputbe.entity.Student;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByName(String name);
    List<Student> findAllByOrderByIdAsc();
}
