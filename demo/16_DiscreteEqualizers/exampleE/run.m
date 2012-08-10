%% Last example for a 1d equalizer

%% Model: X_i, i=1..n are generated from [-1;1] with probability 1/2
%% (BPSK modulation)
%% Observations:
%% Y_i = a_1 X_{i-1} + a_2 X_i + a_3 X_{i+1} + N(0,\sigma^2)
%% Goal: from measurements Y_i, find most likely values for X_i

%% MAP equalizer obtained by splitting and joining variables
%% (equivalent to the junction tree or a third order markov chain)
%% We add the discretized variables Y_i of exampleB, so that the
%% demapper is very lightly suboptimal
global mask sigma

argmax=@(v) find(v==max(v));
argmin=@(v) find(v==min(v));

N=257;  %number of variables. Boundary conditions: X_0=X_1025=1
sigma=0.4;


%% Generating template problems first to find statistics of measurements Y
mask=[0.3 1 0.3];
BPSK=[-1;1];


realYstat=[];

for k=1:100
    realX=BPSK(randi([1 2],1,N))';
    
    realY=zeros(1,N);
    
    %boundary condition first:
    localX=[1 realX(1) realX(2)];
    realY(1)=sum(localX.*mask);
    localX=[realX(N-1) realX(N)  1];
    realY(N)=sum(localX.*mask);
    
    %other points
    for i=2:N-1
        localX=realX(i-1:i+1);
        val=(sum(localX.*mask));
        realY(i)=val;
    end
    realG=sigma*randn(1,N);
    realY=realY+realG;
    
    realYstat=[realYstat realY];
end

%% Construction of Y's discretization

numpoints=40; %number of distinct values Y can take
pp=0:100/(numpoints-1):100;
Ydomain=percentile(realYstat,pp);




% Factor Graph

fg=FactorGraph();
X=Variable([-1;1],1,N);
Y=Variable(Ydomain,1,N);


Xboundary=Variable([-1;1],1,1);
Xboundary.Input=[0 1];
Xboundary2=Variable([-1;1],1,1);
Xboundary2.Input=[0 1];
% Xboundary forced to be %1%
factors=cell(N,1);
%boundary conditions
factors{1}=fg.addFactor(@softjoint,[Xboundary X(1) X(2)],Y(1));
factors{N}=fg.addFactor(@softjoint,[X(N-1) X(N) Xboundary2],Y(N));


for i=2:N-1
    factors{i}=fg.addFactor(@softjoint,X(i-1:i+1),Y(i));
end




% Now splitting each variable into two identical copies
% Xi splits into one additional copy Xi2
% The factor f{i} : X_{i-1} - X_i - X_{i+1} becomes a factor
% between joint variables [X_{i-1,1} X_{i,2}] and [X_{i,1} X_{i+1 2}]
% Conversely: X_i used to be connected to f_{i-1},f_i and f_{i+1}
% f_{i-1} goes to X_{i,2}, f{i+1} goes to X{i,1}
% f_{i} can go to either - we choose arbitrarily X_{i,1}

X2={};
f2={};
for i=2:N
    [X2{i},f2{i}]=fg.split(X(i),factors{i-1});
end

% Next step is to join variables X_{i,1} and X_{i+1,2}
JointX={};
for i=1:N-1
    jointX{i}=fg.join(X(i),X2{i+1});
end

%last step is joining multiple factors between pairs of variables
%         ____
%        /    \
%  [Var] ------ [Var2]     ===>    [Var] ------ [Var2]
%
for i=2:N
    fg.join(factors{i},f2{i});
end

fg.Solver = 'SumProduct';


numtrials=20;

successes=zeros(numtrials,1);
tic;
fprintf('Running %d trials\n',numtrials); 
for k=1:numtrials
    
    fprintf('Trial number:%d\n',k);

    %Problem generation
    realX=BPSK(randi([1 2],1,N))';
    realY=zeros(1,N);
    
    %boundary condition first:
    localX=[1 realX(1) realX(2)];
    realY(1)=sum(localX.*mask);
    localX=[realX(N-1) realX(N)  1];
    realY(N)=sum(localX.*mask);
    
    %other points
    for i=2:N-1
        localX=realX(i-1:i+1);
        val=(sum(localX.*mask));
        realY(i)=val;
    end
    realG=sigma*randn(1,N);
    realY=realY+realG;
    
    
    %inputs
    
    for i=1:N
        idx=argmin(abs(Ydomain-realY(i)));
        iin=zeros(1,length(Ydomain));
        iin(idx)=1;
        Y(i).Input=iin;
    end
    
    fg.Solver.iterate(1);%it's tree so it needs only one iteration (with tree scheduler)
    final_values=zeros(1,N);
    for i=1:N-1
        doublevalue=argmax(jointX{i}.Belief);
        final_values(i)=-1+2*((doublevalue==2)||(doublevalue==4));
    end
    final_values(N)=-1+2*((doublevalue==3)||(doublevalue==4));
    succ_final=sum(final_values==realX);
    
    successes(k)=succ_final;
    
end
toc;
fprintf('Final BER:%d\n',(N-mean(successes))/N);
fprintf('Throughput: %d bits per second\n',N/toc*numtrials);
