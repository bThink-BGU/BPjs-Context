bp.registerBThread("PopulateDB", function() {
    var board = [];
    var triples = [];

    for(var i=0; i<3; i++){
        var row = [];
        for(var j=0; j<3; j++){
            var cell = new Cell(i,j);
            row.push(cell);
        }
        board.push(row);
    }

    var diag1= [];
    var diag2= [];
    for(var i=0; i<3; i++) {
        diag1.push(board[i][i]);
        diag2.push(board[i][2-i]);
        var row = [];
        var col = [];
        for (var j = 0; j < 3; j++) {
            row.push(board[i][j]);
            col.push(board[j][i]);
        }
        triples.push(new Triple("row_"+i, row));
        triples.push(new Triple("col_"+i, col));
    }
    triples.push(new Triple("diag_1", diag1));
    triples.push(new Triple("diag_2", diag2));

    // flattening board
    var cells = [].concat.apply([], board);

    bp.sync({ request: CTX.InsertEvent(cells) });
    bp.sync({ request: CTX.InsertEvent(triples) });
});