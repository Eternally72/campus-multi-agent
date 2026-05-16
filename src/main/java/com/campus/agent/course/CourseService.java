package com.campus.agent.course;

import com.campus.agent.user.AppUser;
import com.campus.agent.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courses;
    private final AppUserRepository users;

    @Transactional
    public CourseResponse create(Long userId, CreateCourseRequest request) {
        AppUser owner = users.getReferenceById(userId);
        Course course = new Course();
        course.setOwner(owner);
        course.setName(request.name());
        course.setTerm(request.term());
        course.setTeacher(request.teacher());
        return CourseResponse.from(courses.save(course));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> list(Long userId) {
        return courses.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
            .map(CourseResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Course getOwnedCourse(Long userId, Long courseId) {
        Course course = courses.findById(courseId).orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        if (!course.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该课程");
        }
        return course;
    }
}
