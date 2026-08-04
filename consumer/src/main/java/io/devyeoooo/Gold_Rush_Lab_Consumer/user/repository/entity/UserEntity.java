package io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.entity;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.MineEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mine_id", nullable = false)
    private MineEntity mine;

    private Long totalMinedGold;

    private UUID sessionId;

    public void addGold(Long amount) {
        totalMinedGold += amount;
    }
}
