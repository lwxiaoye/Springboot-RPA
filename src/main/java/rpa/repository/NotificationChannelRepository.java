package rpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rpa.entity.NotificationChannel;
import java.util.List;

@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {

    List<NotificationChannel> findByChannelType(String channelType);

    List<NotificationChannel> findByEnabled(Boolean enabled);

    @Query("SELECT nc FROM NotificationChannel nc WHERE nc.isDefault = true AND nc.enabled = true")
    List<NotificationChannel> findByIsDefaultTrue();

    long countByEnabled(Boolean enabled);
}
