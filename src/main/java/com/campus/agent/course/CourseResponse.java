package com.campus.agent.course;

import java.time.Instant;

public record CourseResponse(Long id, String name, String term, String teacher, Instant createdAt) {

    static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getName(), course.getTerm(), course.getTeacher(), course.getCreatedAt());
    }
}
