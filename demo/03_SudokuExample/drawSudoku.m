%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Copyright (c) 2010, Lyric Semiconductor, Inc.
% All rights reserved.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function drawSudoku(s)

    numWidth = 100;
    numHeight = 100;
    boardHeight = numHeight*9;
    boardWidth = numWidth*9;

    hold off;
    %figure;
    plot(1,1);
    hold on;

    for i = 1:9
        imgs{i} = flipdim(imread(['images/num' num2str(i) '.jpg']),1);
    end

    for row = 1:9
        for col = 1:9
            xLeft = (col-1)*numWidth+1;
            xRight = xLeft+numWidth;
            yBottom = (row-1)*numHeight+1;
            yTop = yBottom+numHeight;
            b = s(row,col).Belief;
            for i = 1:9
                %rgb = imread(['num' num2str(i) '.jpg']);
                m = image(imgs{i},'XData',[xLeft xRight],'YData',[yBottom yTop ]);
                alpha(m,b(i));
            end

        end
    end

    for i = 1:10
        x = (i-1)*numWidth+1;
        if mod(i,3) == 1
            w = 2;
        else
            w = 1;
        end
        plot([x x],[0 boardHeight],'Color','black','LineWidth',w);
        y = (i-1)*numHeight+1;
        plot([0 boardWidth],[y y],'Color','black','LineWidth',w);
    end
end
