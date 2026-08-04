package io.devyeoooo.Gold_Rush_Lab_Consumer.mining_log.repository.entity;

import io.devyeoooo.Gold_Rush_Lab_Consumer.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab_Consumer.user.repository.entity.UserEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mining_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiningLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mine_id", nullable = false)
    private MineEntity mine;

    private Long amount;

    private MiningLogEntity(UserEntity user, MineEntity mine, Long amount) {
        this.user = user;
        this.mine = mine;
        this.amount = amount;
    }

    public static MiningLogEntity create(UserEntity user, MineEntity mine, Long amount) {
        return new MiningLogEntity(user, mine, amount);
    }
}
