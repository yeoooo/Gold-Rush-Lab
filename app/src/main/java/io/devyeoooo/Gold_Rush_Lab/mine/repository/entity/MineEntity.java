package io.devyeoooo.Gold_Rush_Lab.mine.repository.entity;

import io.devyeoooo.Gold_Rush_Lab.comm.BaseEntity;
import io.devyeoooo.Gold_Rush_Lab.comm.exception.MineDepletedException;
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

    @Version
    Long version;

    public static MineEntity create(Long remainingAmount) {
        if(remainingAmount == null || remainingAmount < 0) {
            throw new IllegalArgumentException("Mine의 잔량은 음수이거나 null 일 수 없습니다.");
        }

        return new MineEntity(null, remainingAmount, null);
    }

    public void mine(Long amount) {
        if(amount == null || amount <= 0) {
            throw new IllegalArgumentException("채굴량은 양수여야합니다..");
        }

        if (this.remainingAmount - amount < 0) {
            throw new MineDepletedException();
        }
        this.remainingAmount -= amount;
    }
}
