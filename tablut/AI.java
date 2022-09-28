package tablut;

import static java.lang.Math.*;

import java.util.List;




import static tablut.Board.*;
import static tablut.Piece.*;
import static tablut.Square.*;

/** A Player that automatically generates moves.
 *  @author Aarini
 */
class AI extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A position-score magnitude indicating a forced win in a subsequent
     *  move.  This differs from WINNING_VALUE to avoid putting off wins. */
    private static final int WILL_WIN_VALUE = Integer.MAX_VALUE - 40;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI with no piece or controller (intended to produce
     *  a template). */
    AI() {
        this(null, null);
    }

    /** A new AI playing PIECE under control of CONTROLLER. */
    AI(Piece piece, Controller controller) {
        super(piece, controller);
    }

    @Override
    Player create(Piece piece, Controller controller) {
        return new AI(piece, controller);
    }

    @Override
    String myMove() {
        Move move = findMove();
        _controller.reportMove(move);
        return move.toString();
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        _lastFoundMove = null;
        if (_myPiece == WHITE || _myPiece == KING) {
            findMove(b, maxDepth(b), true, 1, -INFTY, INFTY);
        } else if (_myPiece == BLACK) {
            findMove(b, maxDepth(b), true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        if (board.winner() != null || depth == 0) {
            return staticScore(board);
        } else {
            if (sense == 1) {
                int best = -INFTY;
                for (Move next : board.legalMoves(WHITE)) {
                    Board b = new Board(board);
                    b.makeMove(next);
                    int response = findMove(b, depth - 1,
                            false, -sense, alpha, beta);
                    if (response > best) {
                        best = response;
                        if (saveMove) {
                            _lastFoundMove = next;
                        }
                        alpha = max(alpha, best);
                        if (alpha > beta) {
                            break;
                        }
                    }
                }
                return best;

            } else if (sense == -1) {
                int worst = INFTY;
                for (Move next : board.legalMoves(BLACK)) {
                    Board b = new Board(board);
                    b.makeMove(next);
                    int response = findMove(b, depth - 1,
                            false, -sense, alpha, beta);
                    if (response < worst) {
                        worst = response;
                        if (saveMove) {
                            _lastFoundMove = next;
                        }
                        beta = min(beta, worst);
                        if (alpha > beta) {
                            break;
                        }
                    }
                }
                return worst;
            } else {
                throw new Error("Wrong sense provided.");
            }
        }
    }


    /** Return a heuristically determined maximum search depth
     *  based on characteristics of BOARD. */
    private static int maxDepth(Board board) {
        if (board.moveLimit() > 0) {
            return board.moveLimit();
        }
        int n = board.legalMoves(board.turn()).size();
        return (n / ((10 * 3) + 8)) + 1;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int wScore = 0;
        int bScore = 0;
        if (board.winner() == WHITE) {
            return WINNING_VALUE;
        } else if (board.winner() == BLACK) {
            return -WINNING_VALUE;
        }
        for (int i = 0; i < NUM_SQUARES; i++) {
            for (int j = 0; j < NUM_SQUARES; j++) {
                if (board.get(sq(i)) == KING) {
                    if (sq(i).isRookMove(sq(j)) && sq(j).isEdge()) {
                        if (board.turn() == WHITE) {
                            wScore += 1000;
                        } else {
                            wScore += 100;
                        }
                    }
                }
            }
        }
        if (board.get(THRONE) != KING || board.get(NTHRONE) != KING
                || board.get(ETHRONE) != KING || board.get(STHRONE) != KING
                || board.get(WTHRONE) != KING) {
            bScore += 10;
        }
        Square[] sq = new Square[] {NTHRONE, ETHRONE, WTHRONE, STHRONE};
        int x = 0;
        for (Square s : sq) {
            if (board.get(s) == BLACK) {
                x++;
            }
        }
        if (board.get(THRONE) == KING) {
            bScore += (x * 5);
        }
        List<Move> object1 = board.legalMoves(BLACK);
        for (Move M :  object1) {
            bScore++;
        }
        List<Move> object = board.legalMoves(WHITE);
        for (Move M : object) {
            wScore++;
        }
        return wScore - bScore;

    }

}


