package team.project.myTeam;

import java.awt.Rectangle;
import java.util.ArrayList;

import easy_soccer_lib.PlayerCommander;
import easy_soccer_lib.perception.FieldPerception;
import easy_soccer_lib.perception.MatchPerception;
import easy_soccer_lib.perception.PlayerPerception;
import easy_soccer_lib.utils.EFieldSide;
import easy_soccer_lib.utils.Vector2D;

public class CommandPlayer extends Thread {

	private int LOOP_INTERVAL = 100; // 0.1s
	private PlayerCommander commander;
	private PlayerPerception selfPerc;
	private FieldPerception fieldPerc;
	private MatchPerception matchPerc;
	private boolean ballPossession = false;

	public CommandPlayer(PlayerCommander player) {
		commander = player;
	}

	@Override
	public void run() {
		System.out.println(">> Executando...");
		long nextIteration = System.currentTimeMillis() + LOOP_INTERVAL;
		updatePerceptions();
		switch (selfPerc.getUniformNumber()) {
		case 1:
			acaoGoleiro(nextIteration);
			break;
		case 2:
			if (selfPerc.getSide() == EFieldSide.LEFT) {
				acaoZagueiro(nextIteration, -1); // cima
			} else {
				acaoZagueiro(nextIteration, 1); // baixo
			}
			break;
		case 3:
			if (selfPerc.getSide() != EFieldSide.LEFT) {
				acaoZagueiro(nextIteration, -1); // cima
			} else {
				acaoZagueiro(nextIteration, 1); // baixo
			}
			break;
		case 4:
			if (selfPerc.getSide() == EFieldSide.LEFT) {
				acaoArmador(nextIteration, -1); // cima
			} else {
				acaoArmador(nextIteration, 1); // baixo
			}
			break;
		case 5:
			acaoArmador(nextIteration, 0); // meio
			break;
		case 6:
			if (selfPerc.getSide() != EFieldSide.LEFT) {
				acaoArmador(nextIteration, -1); // cima
			} else {
				acaoArmador(nextIteration, 1); // baixo
			}
			break;
		case 7:
			if (selfPerc.getSide() == EFieldSide.LEFT) {
				acaoAtacante(nextIteration, -1); // cima
			} else {
				acaoAtacante(nextIteration, 1); // baixo
			}
			break;
		case 8:
			if (selfPerc.getSide() != EFieldSide.LEFT) {
				acaoAtacante(nextIteration, -1); // cima
			} else {
				acaoAtacante(nextIteration, 1); // baixo
			}
			break;
		default:
			break;
		}
	}

	private void updatePerceptions() {
		PlayerPerception newSelf = commander.perceiveSelfBlocking();
		FieldPerception newField = commander.perceiveFieldBlocking();
		MatchPerception newMatch = commander.perceiveMatchBlocking();
		if (newSelf != null)
			this.selfPerc = newSelf;
		if (newField != null)
			this.fieldPerc = newField;
		if (newMatch != null)
			this.matchPerc = newMatch;
	}

	private void turnToPoint(Vector2D point) {
		Vector2D newDirection = point.sub(selfPerc.getPosition());
		commander.doTurnToDirection(newDirection);
	}

	private boolean isAlignToPoint(Vector2D point, double margin) {
		double angle = point.sub(selfPerc.getPosition()).angleFrom(selfPerc.getDirection());
		return angle < margin && angle > margin * (-1);
	}

	private void kickToPoint(Vector2D point, double intensity) {
		Vector2D newDirection = point.sub(selfPerc.getPosition());
		double angle = newDirection.angleFrom(selfPerc.getDirection());
		if (angle > 90 || angle < -90) {
			commander.doTurnToDirection(newDirection);
			angle = 0;
		}
		commander.doKick(intensity, angle);
	}

	private boolean isPointsAreClose(Vector2D reference, Vector2D point, double margin) {
		return reference.distanceTo(point) <= margin;
	}

	private PlayerPerception getClosestPlayerPoint(Vector2D point, EFieldSide side, double margin) {
		ArrayList<PlayerPerception> lp = fieldPerc.getTeamPlayers(side);
		PlayerPerception np = null;
		if (lp != null && !lp.isEmpty()) {
			double dist, temp;
			dist = lp.get(0).getPosition().distanceTo(point);
			np = lp.get(0);
			if (isPointsAreClose(np.getPosition(), point, margin))
				return np;
			for (PlayerPerception p : lp) {
				if (p.getPosition() == null)
					break;
				if (isPointsAreClose(p.getPosition(), point, margin))
					return p;
				temp = p.getPosition().distanceTo(point);
				if (temp < dist) {
					dist = temp;
					np = p;
				}
			}
		}
		return np;
	}

	private boolean isBallPossession() {
		return this.ballPossession;
	}

	private void setBallPossession(boolean ballPossession) {
		this.ballPossession = ballPossession;
	}

	private void dash(Vector2D point, int speed) {
		if (selfPerc.getPosition().distanceTo(point) <= 1)
			return;
		if (!isAlignToPoint(point, 15))
			turnToPoint(point);
		// commander.doMove(x, y)
		commander.doDash(speed);
	}

	private PlayerPerception getClosestPlayerPoint(Vector2D point, EFieldSide side, double margin, int ignoreUniform) {
		ArrayList<PlayerPerception> lp = fieldPerc.getTeamPlayers(side);
		PlayerPerception np = null;
		if (lp != null && !lp.isEmpty()) {
			double dist = 0, temp;
			dist = lp.get(0).getPosition().distanceTo(point);
			np = lp.get(0);

			if (isPointsAreClose(np.getPosition(), point, margin))
				return np;
			for (PlayerPerception p : lp) {
				if (p.getUniformNumber() != ignoreUniform) {
					if (p.getPosition() == null)
						break;
					if (isPointsAreClose(p.getPosition(), point, margin))
						return p;
					temp = p.getPosition().distanceTo(point);
					if (temp < dist) {
						dist = temp;
						np = p;
					}
				}
			}
		}
		return np;
	}

	private void acaoGoleiro(long nextIteration) {
		double xInit = -51, yInit = 0, ballX = 0, ballY = 0;
		EFieldSide side = selfPerc.getSide();
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D centerPos = new Vector2D(0, 0);
		Vector2D ballPos;
		Rectangle area = side == EFieldSide.LEFT ? new Rectangle(-52, -20, 16, 40) : new Rectangle(36, -20, 16, 40);
		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();
			switch (matchPerc.getState()) {
			case BEFORE_KICK_OFF:
				// posiciona
				commander.doMoveBlocking(xInit, yInit);
				break;
			case KICK_OFF_LEFT:
			case KICK_OFF_RIGHT:
				dash(initPos, 90);
				break;
			case GOAL_KICK_LEFT:
				if (side == EFieldSide.LEFT) {
					dash(ballPos, 90);
					if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
						// chutar
						kickToPoint(centerPos, 90);
					}
				}
			case GOAL_KICK_RIGHT:
				if (side == EFieldSide.RIGHT) {
					dash(ballPos, 90);
					if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
						// chutar
						kickToPoint(centerPos, 90);
					}
				}
			case PLAY_ON:
				ballX = fieldPerc.getBall().getPosition().getX();
				ballY = fieldPerc.getBall().getPosition().getY();
				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
					// chutar
					kickToPoint(centerPos, 90);
				} else if (area.contains(ballX, ballY)) {
					// defender
					Vector2D goTo = new Vector2D(initPos.getX(), ballPos.getY());
					dash(goTo, 90);
				} else {
					dash(initPos, 60);
					turnToPoint(ballPos);
				}
				break;
			/* Todos os estados da partida */
			default:
				break;
			}
		}
	}

	private void acaoZagueiro(long nextIteration, int pos) {
		double xInit = -30, yInit = 8 * pos;
		EFieldSide side = selfPerc.getSide();
		EFieldSide enemySide = side.invert(side);
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D ballPos, vTemp;
		Vector2D goalPos = new Vector2D(50 * side.value(), 0);
		Vector2D bestDefenseSpot = new Vector2D(-35 * side.value(), yInit * side.value());
		Vector2D bestAttackSpot = new Vector2D(5 * side.value(), yInit * side.value());
		PlayerPerception pTemp;
		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();
			ArrayList<PlayerPerception> myTeam = fieldPerc.getTeamPlayers(side);
			ArrayList<PlayerPerception> enemyTeam = fieldPerc.getTeamPlayers(enemySide);
			switch (matchPerc.getState()) {
			case BEFORE_KICK_OFF:
				commander.doMoveBlocking(xInit, yInit);
				break;
			case KICK_IN_LEFT:
			case OFFSIDE_LEFT:
			case FREE_KICK_LEFT:
				if (side == EFieldSide.LEFT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						PlayerPerception closestPlayer = selfPerc;
						double closestDistance = Double.MAX_VALUE;

						for (PlayerPerception player : myTeam) {
							double playerDistance = player.getPosition().distanceTo(ballPos);
							if (playerDistance > 1) {
								if (playerDistance < closestDistance) {
									closestDistance = playerDistance;
									closestPlayer = player;
								}
							}
						}

						vTemp = closestPlayer.getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						dash(bestAttackSpot, 90);
					}
				} else {
					dash(bestDefenseSpot, 90);
				}
				break;
			case KICK_IN_RIGHT:
			case OFFSIDE_RIGHT:
			case FREE_KICK_RIGHT:
				if (side == EFieldSide.RIGHT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						PlayerPerception closestPlayer = selfPerc;
						double closestDistance = Double.MAX_VALUE;

						for (PlayerPerception player : myTeam) {
							double playerDistance = player.getPosition().distanceTo(ballPos);
							if (playerDistance > 1) {
								if (playerDistance < closestDistance) {
									closestDistance = playerDistance;
									closestPlayer = player;
								}
							}
						}

						vTemp = closestPlayer.getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						// dash to best attack position
					}
				} else {
					// dash to best defense position
					dash(initPos, 90);
				}
				break;
			case CORNER_KICK_LEFT:
				if (side == EFieldSide.LEFT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						vTemp = getClosestPlayerPoint(goalPos, side, 100).getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						// dash to best attack position
					}
				} else {
					// dash to best defense position
					dash(initPos, 90);
				}
				break;
			case CORNER_KICK_RIGHT:
				if (side == EFieldSide.RIGHT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						vTemp = getClosestPlayerPoint(goalPos, side, 100).getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						// dash to best attack position
					}
				} else {
					// dash to best defense position
					dash(initPos, 90);
				}
				break;
			case KICK_OFF_LEFT:
				dash(initPos, 90);
				break;
			case KICK_OFF_RIGHT:
				dash(initPos, 90);
				break;
			case PLAY_ON:
				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
					PlayerPerception closestPlayer = selfPerc;
					double closestDistance = Double.MAX_VALUE;

					for (PlayerPerception player : myTeam) {
						double playerDistance = player.getPosition().distanceTo(ballPos);
						if (playerDistance > 1) {
							if (playerDistance < closestDistance && player.getUniformNumber() > 3) {
								closestDistance = playerDistance;
								closestPlayer = player;
							}
						}
					}

					vTemp = closestPlayer.getPosition();
					System.out.println(vTemp);

					Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
					double intensity = ((vTempF.magnitude() * 90) / 40);
					if (intensity > 60)
						intensity = 10;
					kickToPoint(vTemp, intensity);
				} else {
					pTemp = getClosestPlayerPoint(ballPos, side, 5);
					if (pTemp != null && pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						// pega a bola
						dash(ballPos, 90);
					} else if (isPointsAreClose(selfPerc.getPosition(),
							getClosestPlayerPoint(selfPerc.getPosition(), enemySide, 10).getPosition(), 8)) {
						if (selfPerc.getPosition().distanceTo(initPos) > 10) {
							dash(initPos, 90);
						} else {
							dash(getClosestPlayerPoint(selfPerc.getPosition(), enemySide, 8).getPosition(), 90);
						}
					} else if (!isPointsAreClose(selfPerc.getPosition(), initPos, 3)) {
						// recua
						dash(initPos, 90);
					} else {
						// olha para a bola
						if (ballPos.getX() > 0) {
							if (side == EFieldSide.LEFT) {
								dash(bestAttackSpot, 90);
							} else {
								dash(bestDefenseSpot, 90);
							}
						} else {
							if (side == EFieldSide.LEFT) {
								dash(bestDefenseSpot, 90);
							} else {
								dash(bestAttackSpot, 90);
							}
						}
						turnToPoint(ballPos);
					}
				}
				break;
			/* Todos os estados da partida */
			default:
				break;
			}
		}
	}

	private void acaoArmador(long nextIteration, int pos) {
		double xInit = -15, yInit = 15 * pos;
		EFieldSide side = selfPerc.getSide();
		EFieldSide enemySide = side.invert(side);
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D ballPos, vTemp;
		Vector2D goalPos = new Vector2D(50 * side.value(), 0);
		Vector2D bestDefenseSpot = new Vector2D(-25 * side.value(), 10 * pos * side.value());
		Vector2D bestAttackSpot = new Vector2D(15 * side.value(), 23 * pos * side.value());
		PlayerPerception pTemp;
		while (true) {
			updatePerceptions();
			ballPos = fieldPerc.getBall().getPosition();
			ArrayList<PlayerPerception> myTeam = fieldPerc.getTeamPlayers(side);
			ArrayList<PlayerPerception> enemyTeam = fieldPerc.getTeamPlayers(enemySide);
			switch (matchPerc.getState()) {

			case BEFORE_KICK_OFF:
				commander.doMoveBlocking(xInit, yInit);
				break;
			case KICK_IN_LEFT:
			case OFFSIDE_LEFT:
			case FREE_KICK_LEFT:
				if (side == EFieldSide.LEFT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						PlayerPerception closestPlayer = selfPerc;
						double closestDistance = Double.MAX_VALUE;

						for (PlayerPerception player : myTeam) {
							double playerDistance = player.getPosition().distanceTo(ballPos);
							if (playerDistance > 1) {
								if (playerDistance < closestDistance) {
									closestDistance = playerDistance;
									closestPlayer = player;
								}
							}
						}

						vTemp = closestPlayer.getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						dash(bestAttackSpot, 90);
					}
				} else {
					dash(bestDefenseSpot, 90);
				}
				break;
			case KICK_IN_RIGHT:
			case OFFSIDE_RIGHT:
			case FREE_KICK_RIGHT:
				if (side == EFieldSide.RIGHT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						PlayerPerception closestPlayer = selfPerc;
						double closestDistance = Double.MAX_VALUE;

						for (PlayerPerception player : myTeam) {
							double playerDistance = player.getPosition().distanceTo(ballPos);
							if (playerDistance > 1) {
								if (playerDistance < closestDistance) {
									closestDistance = playerDistance;
									closestPlayer = player;
								}
							}
						}

						vTemp = closestPlayer.getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						// dash to best attack position
					}
				} else {
					// dash to best defense position
					dash(initPos, 90);
				}
				break;
			case CORNER_KICK_LEFT:
				if (side == EFieldSide.LEFT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						vTemp = getClosestPlayerPoint(goalPos, side, 100).getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						// dash to best attack position
					}
				} else {
					// dash to best defense position
					dash(initPos, 90);
				}
				break;
			case CORNER_KICK_RIGHT:
				if (side == EFieldSide.RIGHT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						vTemp = getClosestPlayerPoint(goalPos, side, 100).getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					} else {
						// dash to best attack position
					}
				} else {
					// dash to best defense position
					dash(initPos, 90);
				}
				break;
			case KICK_OFF_LEFT:
				dash(initPos, 90);
				break;
			case KICK_OFF_RIGHT:
				dash(initPos, 90);
				break;
			case PLAY_ON:
				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
					PlayerPerception closestPlayer = selfPerc;
					double closestDistance = Double.MAX_VALUE;

					for (PlayerPerception player : myTeam) {
						double playerDistance = player.getPosition().distanceTo(ballPos);
						if (playerDistance > 1) {
							if (playerDistance < closestDistance && player.getUniformNumber() > 4
									&& player.getUniformNumber() != 5) {

								closestDistance = playerDistance;
								closestPlayer = player;
							}
						}
					}

					vTemp = closestPlayer.getPosition();
					System.out.println(vTemp);

					Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
					double intensity = ((vTempF.magnitude() * 90) / 40);
					if (intensity > 50)
						intensity = 15;
					kickToPoint(vTemp, intensity);
				} else {
					pTemp = getClosestPlayerPoint(ballPos, side, 5);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						// pega a bola
						dash(ballPos, 90);
					} else if (isPointsAreClose(selfPerc.getPosition(),
							getClosestPlayerPoint(selfPerc.getPosition(), enemySide, 10).getPosition(), 5)) {
						if (selfPerc.getPosition().distanceTo(initPos) > 10) {
							dash(initPos, 90);
						} else {
							dash(getClosestPlayerPoint(selfPerc.getPosition(), enemySide, 5).getPosition(), 90);
						}
					} else {
						// olha para a bola
						if (ballPos.getX() > 0) {
							if (side == EFieldSide.LEFT) {
								dash(bestAttackSpot, 90);
							} else {
								dash(bestDefenseSpot, 90);
							}
						} else {
							if (side == EFieldSide.LEFT) {
								dash(bestDefenseSpot, 90);
							} else {
								dash(bestAttackSpot, 90);
							}
						}
						turnToPoint(ballPos);
					}
				}
				break;
			/* Todos os estados da partida */
			default:
				break;
			}
		}
	}

	private void acaoAtacante(long nextIteration, int pos) {
		double xInit = -7, yInit = 8 * pos;
		EFieldSide side = selfPerc.getSide();
		EFieldSide enemySide = side.invert(side);
		Vector2D initPos = new Vector2D(xInit * side.value(), yInit * side.value());
		Vector2D goalPos = new Vector2D(50 * side.value(), 0);
		Vector2D ballPos, vTemp;
		Vector2D bestAttackSpot = new Vector2D(30 * side.value(), yInit * side.value());
		Vector2D bestDefenseSpot = new Vector2D(-15 * side.value(), yInit * side.value());
		Vector2D centerPos = new Vector2D(0 * side.value(), 0);
		PlayerPerception pTemp;
		while (true) {
			updatePerceptions();
			ArrayList<PlayerPerception> myTeam = fieldPerc.getTeamPlayers(side);
			ballPos = fieldPerc.getBall().getPosition();
			switch (matchPerc.getState()) {
			case BEFORE_KICK_OFF:
				commander.doMoveBlocking(xInit, yInit);
				break;
			case KICK_OFF_LEFT:
				if (side == EFieldSide.LEFT) {
					pTemp = getClosestPlayerPoint(centerPos, side, 5);
					dash(centerPos, 90);
					PlayerPerception closestPlayer = selfPerc;
					double closestDistance = Double.MAX_VALUE;

					for (PlayerPerception player : myTeam) {
						double playerDistance = player.getPosition().distanceTo(ballPos);
						if (playerDistance > 1) {
							if (playerDistance < closestDistance) {
								closestDistance = playerDistance;
								closestPlayer = player;
							}
						}
					}

					vTemp = closestPlayer.getPosition();
					System.out.println(vTemp);

					Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
					double intensity = ((vTempF.magnitude() * 90) / 40);
					if (intensity > 80)
						intensity = 10;
					kickToPoint(vTemp, intensity);
				} else {
					dash(initPos, 90);
				}
				break;
			case KICK_OFF_RIGHT:
				if (side == EFieldSide.RIGHT) {
					pTemp = getClosestPlayerPoint(centerPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(centerPos, 90);
						PlayerPerception closestPlayer = selfPerc;
						double closestDistance = Double.MAX_VALUE;

						for (PlayerPerception player : myTeam) {
							double playerDistance = player.getPosition().distanceTo(ballPos);
							if (playerDistance > 1) {
								if (playerDistance < closestDistance) {
									closestDistance = playerDistance;
									closestPlayer = player;
								}
							}
						}

						vTemp = closestPlayer.getPosition();
						System.out.println(vTemp);

						Vector2D vTempF = vTemp.sub(selfPerc.getPosition());
						double intensity = ((vTempF.magnitude() * 90) / 40);
						if (intensity > 80)
							intensity = 10;
						kickToPoint(vTemp, intensity);
					}
				} else {
					dash(initPos, 90);
				}
				break;
			case FREE_KICK_LEFT:
				if (side == EFieldSide.LEFT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						kickToPoint(getClosestPlayerPoint(ballPos, side, 3).getPosition(), 40);
					}
				}
			case FREE_KICK_RIGHT:
				if (side == EFieldSide.RIGHT) {
					pTemp = getClosestPlayerPoint(ballPos, side, 3);
					if (pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						dash(ballPos, 90);
						kickToPoint(getClosestPlayerPoint(ballPos, side, 3).getPosition(), 40);
					}
				}
				break;
			case PLAY_ON:
				if (isPointsAreClose(selfPerc.getPosition(), ballPos, 1)) {
					if (getClosestPlayerPoint(goalPos, side, 10).getUniformNumber() != selfPerc.getUniformNumber()
							&& isPointsAreClose(ballPos, getClosestPlayerPoint(ballPos, enemySide, 10).getPosition(),
									5)) {
						double intensity = ((getClosestPlayerPoint(goalPos, side, 10).getPosition().magnitude() * 90)
								/ 40);
						if (intensity > 50)
							intensity = 15;
						kickToPoint(getClosestPlayerPoint(goalPos, side, 10).getPosition(), 60);

					}
					if (isPointsAreClose(ballPos, goalPos, 30)) {
						// chuta para o gol
						Vector2D randomShot = new Vector2D(goalPos.getX(), goalPos.getY() + (Math.random() * 3));
						kickToPoint(randomShot, 90);
					} else {
						// conduz para o gol
						kickToPoint(goalPos, 25);
					}
				} else {
					pTemp = getClosestPlayerPoint(ballPos, side, 5);
					if (pTemp != null && pTemp.getUniformNumber() == selfPerc.getUniformNumber()) {
						// pega a bola
						dash(ballPos, 90);
					} else if (pTemp.getUniformNumber() != selfPerc.getUniformNumber()) {
						dash(bestAttackSpot, 90);
					} else {
						// olha para a bola
						if (ballPos.getX() > 0) {
							if (side == EFieldSide.LEFT) {
								dash(bestAttackSpot, 90);
							} else {
								dash(bestDefenseSpot, 90);
							}
						} else {
							if (side == EFieldSide.LEFT) {
								dash(bestDefenseSpot, 90);
							} else {
								dash(bestAttackSpot, 90);
							}
						}
						turnToPoint(ballPos);
					}
				}
				break;
			/* Todos os estados da partida */
			default:
				break;
			}
		}
	}

}
