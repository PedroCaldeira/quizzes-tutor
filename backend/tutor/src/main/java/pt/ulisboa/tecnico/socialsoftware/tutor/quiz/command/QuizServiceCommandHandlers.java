package pt.ulisboa.tecnico.socialsoftware.tutor.quiz.command;

import io.eventuate.tram.commands.consumer.CommandHandlers;
import io.eventuate.tram.commands.consumer.CommandMessage;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.sagas.participant.SagaCommandHandlersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.common.commands.quiz.DeleteQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.common.commands.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.common.dtos.quiz.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.common.serviceChannels.ServiceChannels;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.QuizService;
import pt.ulisboa.tecnico.socialsoftware.tutor.quiz.domain.Quiz;

import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withFailure;
import static io.eventuate.tram.commands.consumer.CommandHandlerReplyBuilder.withSuccess;

public class QuizServiceCommandHandlers {

    private Logger logger = LoggerFactory.getLogger(QuizServiceCommandHandlers.class);

    @Autowired
    private QuizService quizService;

    /**
     * Create command handlers.
     *
     * <p>Map each command to different functions to handle.
     *
     * @return The {code CommandHandlers} object.
     */
    public CommandHandlers commandHandlers() {
        return SagaCommandHandlersBuilder
                .fromChannel(ServiceChannels.QUIZ_SERVICE_COMMAND_CHANNEL)
                .onMessage(DeleteQuizCommand.class, this::deleteQuiz)
                .onMessage(UpdateQuizCommand.class, this::updateQuiz)
                .build();
    }

    public Message updateQuiz(CommandMessage<UpdateQuizCommand> cm) {
        logger.info("Received UpdateQuizCommand");

        QuizDto quizDto = cm.getCommand().getQuizDto();

        try {
            quizService.updateQuiz(quizDto.getId(), quizDto);
            return withSuccess();
        } catch (Exception e) {
            return withFailure();
        }
    }

    public Message deleteQuiz(CommandMessage<DeleteQuizCommand> cm) {
        logger.info("Received DeleteQuizCommand");

        Integer quizId = cm.getCommand().getQuizId();

        try {
            quizService.removeQuiz(quizId);
            return withSuccess();
        } catch (Exception e) {
            return withFailure();
        }
    }
}
