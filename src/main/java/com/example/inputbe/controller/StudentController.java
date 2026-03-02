package com.example.inputbe.controller;

import com.example.inputbe.dto.ApiResponse;
import com.example.inputbe.dto.CreateStudentRequest;
import com.example.inputbe.dto.StudentItem;
import com.example.inputbe.dto.StudentsResponse;
import com.example.inputbe.entity.Student;
import com.example.inputbe.exception.BadRequestException;
import com.example.inputbe.exception.NotFoundException;
import com.example.inputbe.repository.StudentRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;

    public StudentController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @GetMapping
    public StudentsResponse list() {
        List<StudentItem> items = studentRepository.findAllByOrderByIdAsc().stream()
                .map(s -> new StudentItem(s.getId(), s.getName()))
                .toList();
        return new StudentsResponse(true, items);
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@Valid @RequestBody CreateStudentRequest request) {
        String name = request.name().trim();
        if (studentRepository.existsByName(name)) {
            throw new BadRequestException("student already exists");
        }

        Student student = new Student();
        student.setName(name);
        studentRepository.save(student);

        return ResponseEntity.ok(ApiResponse.ok("created"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("student not found"));
        studentRepository.delete(student);
        return ResponseEntity.ok(ApiResponse.ok("deleted"));
    }
}
