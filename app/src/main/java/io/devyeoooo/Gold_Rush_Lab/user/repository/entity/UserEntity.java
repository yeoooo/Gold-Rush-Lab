package io.devyeoooo.Gold_Rush_Lab.user.repository.entity;

import io.devyeoooo.Gold_Rush_Lab.comm.BaseEntity;
import io.devyeoooo.Gold_Rush_Lab.mine.repository.entity.MineEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "app_user")
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mine_id", nullable = false)
    MineEntity mine;

    Long totalMinedGold;

    UUID sessionId;

    public static UserEntity create(MineEntity mine) {
        if (mine == null) {
            throw new IllegalArgumentException("사용자는 Mine에 소속되어야 합니다.");
        }

        return UserEntity.builder()
                .totalMinedGold(0L)
                .sessionId(UUID.randomUUID())
                .mine(mine)
                .build();
    }
}
