package ru.vvizard.rssspy.botservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.vvizard.rssspy.botservice.domain.ChatVV;

/**
 * Created by igormazevich
 */

@Repository
public interface ChatRepository extends CrudRepository<ChatVV, Long> {
}
