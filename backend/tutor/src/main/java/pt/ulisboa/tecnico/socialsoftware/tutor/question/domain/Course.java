package pt.ulisboa.tecnico.socialsoftware.tutor.question.domain;

import pt.ulisboa.tecnico.socialsoftware.common.dtos.course.CourseType;
import pt.ulisboa.tecnico.socialsoftware.common.dtos.execution.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.common.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.tutor.execution.domain.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.DomainEntity;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.Visitor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.common.exceptions.ErrorMessage.INVALID_NAME_FOR_COURSE;
import static pt.ulisboa.tecnico.socialsoftware.common.exceptions.ErrorMessage.INVALID_TYPE_FOR_COURSE;

@Entity
@Table(name = "courses")
public class Course implements DomainEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private CourseType type;

    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course", fetch=FetchType.LAZY, orphanRemoval=true)
    private final Set<CourseExecution> courseExecutions = new HashSet<>();

    @OneToMany(mappedBy = "course", fetch=FetchType.LAZY, orphanRemoval=true)
    private final Set<Question> questions = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "course", fetch=FetchType.LAZY, orphanRemoval=true)
    private final Set<Topic> topics = new HashSet<>();

    public Course() {}

    public Course(String name, CourseType type) {
        setType(type);
        setName(name);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitCourse(this);
    }


    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new TutorException(INVALID_NAME_FOR_COURSE);

        this.name = name;
    }

    public Set<CourseExecution> getCourseExecutions() {
        return courseExecutions;
    }

    public Set<Question> getQuestions() {
        return questions;
    }

    public Set<Topic> getTopics() {
        return topics;
    }

    public void addCourseExecution(CourseExecution courseExecution) {
        courseExecutions.add(courseExecution);
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    public void addTopic(Topic topic) {
        topics.add(topic);
    }

    public CourseType getType() {
        return type;
    }

    public void setType(CourseType type) {
        if (type == null)
            throw new TutorException(INVALID_TYPE_FOR_COURSE);

        this.type = type;
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", courseExecutions=" + courseExecutions +
                '}';
    }

    public Optional<CourseExecution> getCourseExecution(String acronym, String academicTerm, CourseType type) {
        return getCourseExecutions().stream()
                .filter(courseExecution -> courseExecution.getType().equals(type)
                        && courseExecution.getAcronym().equals(acronym)
                        && courseExecution.getAcademicTerm().equals(academicTerm))
                .findAny();
    }

    public boolean existsCourseExecution(String acronym, String academicTerm, CourseType type) {
        return getCourseExecution(acronym, academicTerm, type).isPresent();
    }

    public CourseExecutionDto getCourseExecutionDto() {
        CourseExecutionDto dto = new CourseExecutionDto();
        dto.setCourseId(getId());
        dto.setCourseType(getType());
        dto.setName(getName());
        return dto;
    }
}