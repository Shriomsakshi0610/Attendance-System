package com.renaissance.event;

import com.renaissance.entity.Teacher;

public class TeacherLoginEvent {
    private final Teacher teacher;

    public TeacherLoginEvent(Teacher teacher) {
        this.teacher = teacher;
    }

    public Teacher getTeacher() {
        return teacher;
    }
}
