package io.devyeoooo.Gold_Rush_Lab.mining_log.repository.entity;

import io.devyeoooo.Gold_Rush_Lab.comm.BaseEntity;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import io.devyeoooo.Gold_Rush_Lab.user.repository.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "mining_log")
public class MiningLogEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity user;

    @ManyToOne
    @JoinColumn(name = "mine_id", nullable = false)
    MineEntity mine;

    Long amount;

    public static MiningLogEntity create(UserEntity user, MineEntity mine, Long amount) {
        if (user == null || mine == null) {
            throw new IllegalArgumentException("User와 Mine은 null 일 수 없습니다.");
        }

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount는 null 이거나 음수가 될 수 없습니다.");
        }

        return MiningLogEntity.builder()
                .user(user)
                .mine(mine)
                .amount(amount)
                .build();
    }

}
