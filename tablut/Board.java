package tablut;

import java.util.Stack;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Formatter;
import java.util.List;

import static tablut.Move.ROOK_MOVES;
import static tablut.Piece.*;
import static tablut.Square.*;
import static tablut.Move.mv;


/** The state of a Tablut Game.
 *  @author Aarini
 */
class Board {

    /** The number of squares on a side of the board. */
    static final int SIZE = 9;

    /** The throne (or castle) square and its four surrounding squares.. */
    static final Square THRONE = sq(4, 4),
            NTHRONE = sq(4, 5),
            STHRONE = sq(4, 3),
            WTHRONE = sq(3, 4),
            ETHRONE = sq(5, 4);

    /** Initial positions of attackers. */
    static final Square[] INITIAL_ATTACKERS = {
            sq(0, 3), sq(0, 4), sq(0, 5), sq(1, 4),
            sq(8, 3), sq(8, 4), sq(8, 5), sq(7, 4),
            sq(3, 0), sq(4, 0), sq(5, 0), sq(4, 1),
            sq(3, 8), sq(4, 8), sq(5, 8), sq(4, 7)
    };

    /** Initial positions of defenders of the king. */
    static final Square[] INITIAL_DEFENDERS = {NTHRONE, ETHRONE,
                                                  STHRONE, WTHRONE,
            sq(4, 6), sq(4, 2), sq(2, 4), sq(6, 4)
    };

    /** Initializes a game board with SIZE squares on a side in the
     *  initial position. */
    Board() {
        init();
    }

    /** Initializes a copy of MODEL. */
    Board(Board model) {
        copy(model);
    }

    /** Copies MODEL into me. */
    void copy(Board model) {
        if (model == this) {
            return;
        }
        init();
        this._arrayBoard = new Piece[BOARD_SIZE][BOARD_SIZE];
        this._arrayBoard = model._arrayBoard.clone();
        this._moveCount = model.moveCount();
        this._capturePiece = model._capturePiece;
        this._board = model._board;
        this._strBoard = model.encodedBoard();
        this._playedMoves = model._playedMoves;
        this._strBoardReps = model._strBoardReps;
        this._sqOld = model._sqOld;
        this._turn = model.turn();
        this._winner = model.winner();
        this._repeated = model._repeated;
        this._pOld = model._pOld;
        this._oneCapturePiece = model._oneCapturePiece;
        this._isCaptured = model._isCaptured;
        this._moveLimit = model._moveLimit;
    }

    /** Clears the board to the initial position. */
    void init() {
        _board.clear();
        _undoStack.clear();
        _arrayBoard = new Piece[BOARD_SIZE][BOARD_SIZE];
        _isCaptured = new Stack<Boolean>();
        _capturePiece = new Stack<Piece>();
        _pOld = new Stack<Piece>();
        _sqOld = new Stack<Square>();
        _oneCapturePiece = new Stack<>();
        _strBoardReps = new HashSet<>();
        _winner = null;
        _moveCount = 0;
        _turn = BLACK;

        for (Square sq: INITIAL_ATTACKERS) {
            initial[sq.col()][sq.row()] = BLACK;
            _arrayBoard[sq.col()][sq.row()] = BLACK;
        }
        initial[THRONE.col()][THRONE.row()] = KING;
        _arrayBoard[THRONE.col()][THRONE.row()] = KING;

        for (Square sq: INITIAL_DEFENDERS) {
            initial[sq.col()][sq.row()] = WHITE;
            _arrayBoard[sq.col()][sq.row()] = WHITE;
        }
        for (int col = 0; col < BOARD_SIZE; col += 1) {
            for (int row = 0; row < BOARD_SIZE; row += 1) {
                if (_arrayBoard[col][row] == null) {
                    _arrayBoard[col][row] = EMPTY;
                }
            }
        }
        for (int col = 0; col < BOARD_SIZE; col += 1) {
            for (int row = 0; row < BOARD_SIZE; row += 1) {
                if (initial[col][row] == null) {
                    initial[col][row] = EMPTY;
                }
            }
        }
        _strBoard = encodedBoard();
        _strBoardReps.add(_strBoard);
    }

    /** Set the move limit to LIM.  It is an error if 2*LIM <= moveCount().
     * @param n */
    void setMoveLimit(int n) {
        if (2 * n <= moveCount()) {
            throw new IllegalArgumentException();
        }
        _moveLimit = n;
    }

    /** Return a Piece representing whose move it is (WHITE or BLACK). */
    Piece turn() {
        return _turn;
    }

    /** Return the winner in the current position, or null if there is no winner
     *  yet. */
    Piece winner() {
        return _winner;
    }

    /** Returns true iff this is a win due to a repeated position. */
    boolean repeatedPosition() {
        return _repeated;
    }

    /** Record current position and set winner() next mover if the current
     *  position is a repeat. */
    private void checkRepeated() {
        if (!_strBoardReps.contains(encodedBoard())) {
            _strBoardReps.add(encodedBoard());
        } else {
            _winner = _turn;
            _repeated = true;
        }
    }

    /** Return the number of moves since the initial position that have not been
     *  undone. */
    int moveCount() {
        return _moveCount;
    }

    /** Return location of the king. */
    Square kingPosition() {
        for (int col = 0; col < BOARD_SIZE; col++) {
            for (int row = 0; row < BOARD_SIZE; row++) {

                if (_arrayBoard[col][row] == KING) {
                    return sq(col, row);
                }
            }
        }
        return null;
    }

    /** Return the contents the square at S. */
    final Piece get(Square s) {
        return get(s.col(), s.row());
    }

    /** Return the contents of the square at (COL, ROW), where
     *  0 <= COL, ROW <= 9. */
    final Piece get(int col, int row) {
        return _arrayBoard[col][row];
    }

    /** Return the contents of the square at COL ROW. */
    final Piece get(char col, char row) {
        return get(row - '1', col - 'a');
    }

    /** Set square S to P. */
    final void put(Piece p, Square s) {
        _arrayBoard[s.col()][s.row()] = p;
    }

    /** Set square S to P and record for undoing. */
    final void revPut(Piece p, Square s) {
        put(p, s);
        _undoStack.add(new Board(this));
        _sqOld.push(s);
        _pOld.push(p);
    }

    /** Set square COL ROW to P. */
    final void put(Piece p, char col, char row) {
        put(p, sq(col - 'a', row - '1'));
    }

    /** Return true iff FROM - TO is an unblocked rook move on the current
     *  board.  For this to be true, FROM-TO must be a rook move and the
     *  squares along it, other than FROM, must be empty. */
    boolean isUnblockedMove(Square from, Square to) {
        if (from.isRookMove(to)) {
            boolean bool  = true;
            int dir0 = from.direction(to);
            int dir1 = DIR[dir0][1];
            int dir2 = DIR[dir0][0];
            if (dir2 != 0 && dir1 == 0) {
                for (int col = from.col() + DIR[dir0][0];
                     col != to.col(); col += DIR[dir0][0]) {
                    if (arr2D()[col][from.row()] != EMPTY) {
                        bool = false;
                    }
                }
            } else if (dir2 == 0 && dir1 != 0) {
                for (int row = from.row() + DIR[dir0][1];
                     row != to.row(); row += DIR[dir0][1]) {
                    if (arr2D()[from.col()][row] != EMPTY) {
                        bool = false;
                    }
                }
            }
            return bool && arr2D()[to.col()][to.row()] == EMPTY;
        }
        return false;
    }

    /** Return true iff FROM is a valid starting square for a move. */
    boolean isLegal(Square from) {
        return get(from) == _turn;
    }

    /** Return true iff FROM-TO is a valid move. */
    boolean isLegal(Square from, Square to) {
        if ((turn() == WHITE && get(from) == KING) || (isLegal(from))
                && arr2D()[to.col()][to.row()] == EMPTY) {
            if (isUnblockedMove(from, to)) {
                if (arr2D()[from.col()][from.row()] != KING) {
                    return to != THRONE;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /** Return true iff MOVE is a legal move in the current
     *  position. */
    boolean isLegal(Move move) {
        return isLegal(move.from(), move.to());
    }

    /** Move FROM-TO, assuming this is a legal move. */
    void makeMove(Square from, Square to) {
        assert isLegal(from, to);
        Piece inPiece = _arrayBoard[from.col()][from.row()];
        revPut(inPiece, to); revPut(EMPTY, from);
        _playedMoves.push(mv(from, to));
        for (int d = 0; d < 4; d++) {
            _isCaptured.push(false);
            if (exists(to.col() + 2 * DIR[d][0],
                    to.row() + 2 * DIR[d][1])) {
                Square toSquare = sq(to.col() + 2 * DIR[d][0],
                        to.row() + 2 * DIR[d][1]);
                Square nextS = sq(to.col() + 2 * DIR[d][0],
                        to.row() + 2 * DIR[d][1]);
                Piece nextP = _arrayBoard[to.col()
                        + 2 * DIR[d][0]][to.row() + 2 * DIR[d][1]];
                Piece middle = _arrayBoard[to.between(nextS).col()]
                        [to.between(nextS).row()];
                Square bet = to.between(nextS);
                if (nextP == turn()
                        || (nextP == EMPTY && toSquare == THRONE)) {
                    makingMove(middle, bet, nextP, inPiece, toSquare, to);
                } else if (nextP == KING && inPiece == BLACK
                        && middle == WHITE && toSquare == THRONE) {
                    Square sideThrone = bet.diag1(THRONE);
                    Square sideThrone1 = bet.diag2(THRONE);
                    if (_arrayBoard[sideThrone1.col()]
                            [sideThrone1.row()] == BLACK
                            && _arrayBoard[sideThrone.col()]
                            [sideThrone.row()] == BLACK) {
                        Square lastSideThrone = sideThrone.diag1(THRONE);
                        if (lastSideThrone == bet) {
                            lastSideThrone = sideThrone.diag2(THRONE);
                        }
                        if (_arrayBoard[lastSideThrone.col()]
                                [lastSideThrone.row()] == BLACK) {
                            capture(to, nextS);
                        }
                    }
                }
            }
        }
        if (kingPosition() != null && kingPosition().isEdge()) {
            _winner = WHITE;
        } else if (kingPosition() == null && turn() == BLACK) {
            _winner = BLACK;
        }
        _moveCount++;
        _turn = turn().opponent();
        if (!hasMove(_turn)) {
            _winner = _turn.opponent();
        }
        _strBoard = encodedBoard();
        checkRepeated();
    }

    /** Helper function for makeMove with hostile squares.
     * @param myPiece our piece*
     * @param bwP piece*
     * @param otherPiece other piece*
     * @param toSquare to square*
     * @param bwS between square*
     * @param to square to move to. */
    void makingMove(Piece bwP, Square bwS, Piece otherPiece,
                    Piece myPiece, Square toSquare, Square to) {
        if ((bwP == KING && myPiece == BLACK && otherPiece == BLACK)
                || bwP == turn().opponent()) {
            if (bwP == KING) {
                if (bwS == THRONE || bwS == ETHRONE || bwS == STHRONE
                        || bwS == NTHRONE || bwS == WTHRONE) {
                    Square d1 = toSquare.diag1(bwS);
                    Square d2 = toSquare.diag2(bwS);
                    if ((_arrayBoard[d1.col()][d1.row()] == myPiece
                            || d1 == THRONE) && (_arrayBoard[d2.col()]
                            [d2.row()] == myPiece || d2 == THRONE)) {
                        capture(to, toSquare);
                    }
                } else {
                    capture(to, toSquare);
                }
            } else {
                capture(to, toSquare);
            }
        }
    }

    /** Move according to MOVE, assuming it is a legal move. */
    void makeMove(Move move) {
        makeMove(move.from(), move.to());
    }

    /** Capture the piece between SQ0 and SQ2, assuming a piece just moved to
     *  SQ0 and the necessary conditions are satisfied. */
    private void capture(Square sq0, Square sq2) {
        Square toCapture = sq0.between(sq2);
        _isCaptured.pop();
        _isCaptured.push(true);
        _oneCapturePiece.push(toCapture);
        _capturePiece.push(get(toCapture));
        put(EMPTY, toCapture);
    }

    /** Undo one move.  Has no effect on the initial board. */
    void undo() {
        if (_moveCount > 0 && !_pOld.isEmpty()) {
            undoPosition();
            _moveCount--;
            Square s = _sqOld.pop();
            Piece p = _pOld.pop();
            if (p != EMPTY) {
                throw new IllegalArgumentException();
            }
            Square s1 = _sqOld.pop();
            Piece p0 = _pOld.pop();
            put(p, s1); put(p0, s);
            for (int i = 3; i >= 0; i--) {
                boolean captured = _isCaptured.pop();
                if (captured) {
                    Square c = _oneCapturePiece.pop();
                    Piece cp = _capturePiece.pop();
                    put(cp, c);
                }
            }
            _turn = turn().opponent();
            _winner = null;
            _strBoard = encodedBoard();
        }
    }

    /** Remove record of current position in the set of positions encountered,
     *  unless it is a repeated position or we are at the first move. */
    private void undoPosition() {
        if (!_repeated && moveCount() > 0) {
            _strBoardReps.remove(encodedBoard());
        }
        _repeated = false;
    }

    /** Clear the undo stack and board-position counts. Does not modify the
     *  current position or win status. */
    void clearUndo() {
        _undoStack.add(new Board(this));
        _sqOld.clear(); _pOld.clear();
        _playedMoves.clear(); _undoStack.clear();
        _isCaptured.clear(); _oneCapturePiece.clear();
        _capturePiece.clear(); _strBoardReps.clear();
        _moveCount = 1;
    }

    /** Return a new mutable list of all legal moves on the current board for
     *  SIDE (ignoring whose turn it is at the moment). */
    List<Move> legalMoves(Piece side) {
        List<Move> mList = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (arr2D()[i][j].side() == side) {
                    for (int d = 0; d < 4; d++) {
                        for (Move M: ROOK_MOVES[sq(i, j).index()][d]) {
                            if (isLegalOne(M)) {
                                mList.add(M);
                            }
                        }
                    }
                }
            }
        }
        return mList;
    }

    /** Return true iff MOVE is a legal move. */
    boolean isLegalOne(Move move) {
        if (arr2D()[move.to().col()][move.to().row()] == EMPTY) {
            if (isUnblockedMove(move.from(), move.to())) {
                if (arr2D()[move.from().col()][move.from().row()] != KING) {
                    return move.to() != THRONE;
                } else {
                    return true;
                }
            }

        }
        return false;
    }

    /** Return true iff SIDE has a legal move. */
    boolean hasMove(Piece side) {
        return !legalMoves(side).isEmpty();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    /** Return a text representation of this Board.  If COORDINATES, then row
     *  and column designations are included along the left and bottom sides.
     */
    String toString(boolean coordinates) {
        Formatter out = new Formatter();
        for (int r = SIZE - 1; r >= 0; r -= 1) {
            if (coordinates) {
                out.format("%2d", r + 1);
            } else {
                out.format("  ");
            }
            for (int c = 0; c < SIZE; c += 1) {
                out.format(" %s", get(c, r));
            }
            out.format("%n");
        }
        if (coordinates) {
            out.format("  ");
            for (char c = 'a'; c <= 'i'; c += 1) {
                out.format(" %c", c);
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** Return the locations of all pieces on SIDE. */
    private HashSet<Square> pieceLocations(Piece side) {
        assert side != EMPTY;
        HashSet<Square> pieces = new HashSet<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if ((_arrayBoard[i][j] == side) || (side == WHITE
                        && _arrayBoard[i][j] == KING)) {
                    pieces.add(sq(i, j));
                }
            }
        }
        return pieces;
    }

    /** returns the Move limit.*/
    int moveLimit() {
        return _moveLimit;
    }

    /** Return the contents of _board in the order of SQUARE_LIST as a sequence
     *  of characters: the toString values of the current turn and Pieces. */
    String encodedBoard() {
        char[] result = new char[Square.SQUARE_LIST.size() + 1];
        result[0] = turn().toString().charAt(0);
        for (Square sq : SQUARE_LIST) {
            result[sq.index() + 1] = get(sq).toString().charAt(0);
        }
        return new String(result);
    }

    /** Piece whose turn it is (WHITE or BLACK). */
    private Piece _turn;
    /** Cached value of winner on this board, or EMPTY if it has not been
     *  computed. */
    private Piece _winner;
    /** Number of (still undone) moves since initial position. */
    private int _moveCount;
    /** True when current board is a repeated position (ending the game). */
    private boolean _repeated;

    /** Potential undo stack for board.*/
    private ArrayList<Board> _undoStack = new ArrayList<>();
    /** 2D representation of board. */
    private Piece[][] _arrayBoard = new Piece[BOARD_SIZE][BOARD_SIZE];

    /** Potential hashmap for squares and piece. */
    private HashMap<Square, Piece> _board = new HashMap<>();

    /** Move limit for setmovelimit.*/
    private int _moveLimit;

    /** Gets 2D array of pieces.
     * @return arr*/
    Piece[][] arr2D() {
        return _arrayBoard;
    }

    /**2D array of Direction. */
    private static final int[][] DIR = {
            { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 }
    };

    /** String representation of current board. */
    private String _strBoard;

    /** Hashset of previous board string representations. */
    private HashSet<String> _strBoardReps = new HashSet<>();

    /** A stack of captured pieces, corresponding to squares. */
    private Stack<Piece> _capturePiece = new Stack<>();

    /** Stack of played moves. */
    private java.util.Stack<Move> _playedMoves = new Stack<>();

    /** New 2D array of Pieces in board size. */
    private Piece[][] initial = new Piece[BOARD_SIZE][BOARD_SIZE];

    /** Boolean for if a piece has been captured.*/
    private Stack<Boolean> _isCaptured = new Stack<>();

    /** Stack of squares of captured pieces. */
    private Stack<Square> _oneCapturePiece = new Stack<>();

    /** Stack of old pieces. */
    private Stack<Piece> _pOld = new Stack<>();

    /** Stack of previous squares. */
    private Stack<Square> _sqOld = new Stack<>();

}
