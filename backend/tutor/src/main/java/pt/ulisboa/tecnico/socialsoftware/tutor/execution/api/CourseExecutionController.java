package pt.ulisboa.tecnico.socialsoftware.tutor.execution.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.ulisboa.tecnico.socialsoftware.common.dtos.execution.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.common.dtos.question.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.common.dtos.question.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.common.dtos.user.Role;
import pt.ulisboa.tecnico.socialsoftware.common.dtos.user.StudentDto;
import pt.ulisboa.tecnico.socialsoftware.common.exceptions.TutorException;
import pt.ulisboa.tecnico.socialsoftware.common.security.token.UserInfo;
import pt.ulisboa.tecnico.socialsoftware.tutor.answer.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.tutor.execution.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.tutor.impexp.domain.TarGZip;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.QuizService;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.Quiz;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.repository.QuizRepository;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.common.exceptions.ErrorMessage.AUTHENTICATION_ERROR;


@RestController
public class CourseExecutionController {

    private static final Logger logger = LoggerFactory.getLogger(CourseExecutionController.class);

    @Autowired
    private CourseExecutionService courseExecutionService;

    @Autowired
    private AnswerService answerService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizRepository quizRepository;

    @Value("${export.dir}")
    private String exportDir;

    @GetMapping("/executions")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_DEMO_ADMIN')")
    public List<CourseExecutionDto> getCourseExecutions(Principal principal) {
        UserInfo userInfo = (UserInfo) ((Authentication) principal).getPrincipal();

        if (userInfo == null) {
            throw new TutorException(AUTHENTICATION_ERROR);
        }

        Role role;
        if (userInfo.getAdmin()) {
            role = Role.ADMIN;
        } else {
            role = userInfo.getRole();
        }

        return courseExecutionService.getCourseExecutions(role);
    }

    @GetMapping("/executions/{courseExecutionId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_DEMO_ADMIN')")
    public CourseExecutionDto getCourseExecutionById(@PathVariable Integer courseExecutionId) {
        return courseExecutionService.getCourseExecutionById(courseExecutionId);
    }

    @PostMapping("/executions/external")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DEMO_ADMIN') and hasPermission(#courseExecutionDto, 'DEMO.ACCESS'))")
    public CourseExecutionDto createExternalCourseExecution(@RequestBody CourseExecutionDto courseExecutionDto) {
        return courseExecutionService.createExternalCourseExecution(courseExecutionDto);
    }

    @DeleteMapping("/executions/{courseExecutionId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DEMO_ADMIN') and hasPermission(#courseExecutionId, 'DEMO.ACCESS'))")
    public void removeCourseExecution(@PathVariable Integer courseExecutionId) {
        courseExecutionService.removeCourseExecution(courseExecutionId);
    }

    @PostMapping("/executions/activate")
    @PreAuthorize("hasRole('ROLE_TEACHER') and hasPermission(#courseExecutionDto, 'EXECUTION.CREATE')")
    public CourseExecutionDto activateCourseExecution(Authentication authentication, @RequestBody CourseExecutionDto courseExecutionDto) {
        CourseExecutionDto result = courseExecutionService.createTecnicoCourseExecution(courseExecutionDto);

        Integer userId = ((UserInfo) authentication.getPrincipal()).getId();

        courseExecutionService.addUserToActivatedTecnicoCourseExecution(userId, result.getCourseExecutionId());

        return result;
    }

    @GetMapping("/executions/{executionId}/anonymize")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DEMO_ADMIN') and hasPermission(#executionId, 'DEMO.ACCESS'))")
    public void anonymizeCourseExecutionUsers(@PathVariable int executionId) {
        courseExecutionService.anonymizeCourseExecutionUsers(executionId);
    }

    @GetMapping("/executions/{executionId}/students")
    @PreAuthorize("hasRole('ROLE_TEACHER') and hasPermission(#executionId, 'EXECUTION.ACCESS')")
    public List<StudentDto> getCourseStudents(@PathVariable int executionId) {
        return courseExecutionService.getStudents(executionId);
    }

    @PostMapping("/executions/{executionId}/users/delete")
    @PreAuthorize("hasRole('ROLE_ADMIN') or (hasRole('ROLE_DEMO_ADMIN') and hasPermission(#executionId, 'DEMO.ACCESS'))")
    public CourseExecutionDto deleteExternalInactiveUsers(@PathVariable Integer executionId, @Valid @RequestBody List<Integer> usersIds) {
        return courseExecutionService.deleteExternalInactiveUsers(executionId, usersIds);
    }

    @GetMapping(value = "/executions/{executionId}/export")
    @PreAuthorize("hasRole('ROLE_TEACHER') and hasPermission(#executionId, 'EXECUTION.ACCESS')")
    public void exportCourseExecutionInfo(HttpServletResponse response, @PathVariable Integer executionId) throws IOException {
        List<Quiz> courseExecutionQuizzes = quizRepository.findQuizzesOfExecution(executionId);
        response.setHeader("Content-Disposition", "attachment; filename=file.tar.gz");
        response.setContentType("application/tar.gz");
        String sourceFolder = exportDir + "/quizzes-" + executionId;
        File file = new File(sourceFolder);
        file.mkdir();
        for (Quiz quiz : courseExecutionQuizzes) {
            answerService.writeQuizAnswers(quiz.getId());
            this.quizService.createQuizXmlDirectory(quiz.getId(), sourceFolder);
        }
        TarGZip tGzipDemo = new TarGZip(sourceFolder);
        tGzipDemo.createTarFile();
        response.getOutputStream().write(Files.readAllBytes(Paths.get(sourceFolder + ".tar.gz")));
        response.flushBuffer();

        deleteDirectory(file);
        deleteDirectory(new File(sourceFolder + ".tar.gz"));
    }

    @PostMapping("/executions/{executionId}/import/questions")
    @PreAuthorize("hasRole('ROLE_TEACHER') and hasPermission(#executionId, 'EXECUTION.ACCESS')")
    public List<QuestionDto> importQuestions(@PathVariable Integer executionId, @RequestParam("file") MultipartFile file) throws IOException {
        return courseExecutionService.importQuestions(file.getInputStream(), executionId);
    }

    @GetMapping("/executions/{courseExecutionId}/topics/available")
    @PreAuthorize("(hasRole('ROLE_TEACHER') or hasRole('ROLE_STUDENT')) and hasPermission(#courseExecutionId, 'EXECUTION.ACCESS')")
    public List<TopicDto> getAvailableTopicsByCourseExecution(@PathVariable int courseExecutionId) {
        return this.courseExecutionService.findAvailableTopicsByCourseExecution(courseExecutionId);
    }

    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

}

