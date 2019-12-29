package com.gjg.leaderboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


interface PlayerRepository extends JpaRepository<Player, UUID> {

}