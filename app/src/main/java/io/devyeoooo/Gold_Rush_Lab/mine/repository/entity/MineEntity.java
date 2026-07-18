package io.devyeoooo.Gold_Rush_Lab.mine.repository.entity;

import io.devyeoooo.Gold_Rush_Lab.comm.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "mine")
public class MineEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long remainingAmount;

    public static MineEntity create(Long remainingAmount) {
        if(remainingAmount == null || remainingAmount < 0) {
            throw new IllegalArgumentException("Mine의 잔량은 음수이거나 null 일 수 없습니다.");
        }

        return new MineEntity(null, remainingAmount);
    }
}
