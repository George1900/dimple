%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%   Copyright 2012 Analog Devices, Inc.
%
%   Licensed under the Apache License, Version 2.0 (the "License");
%   you may not use this file except in compliance with the License.
%   You may obtain a copy of the License at
%
%       http://www.apache.org/licenses/LICENSE-2.0
%
%   Unless required by applicable law or agreed to in writing, software
%   distributed under the License is distributed on an "AS IS" BASIS,
%   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
%   See the License for the specific language governing permissions and
%   limitations under the License.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

classdef VariableBase < Node
    properties (Abstract = true)
        Input;
        Belief;
        Value;
    end
    properties
        Domain;
        Factors;
        FactorsTop;
        FactorsFlat;
        Guess;
    end
    
    methods
        
        function obj = VariableBase(vectorObject,indices)
            obj@Node(vectorObject,indices);
        end
        
        function z= mod(a,b)
            z = addOperatorOverloadedFactor(a,b,@mod,@(z,x,y) z == mod(x,y));
        end
        
        function z = minus(a,b)
            z = addOperatorOverloadedFactor(a,b,@minus,@(z,x,y) z == (x-y));
        end
        
        function z = mpower(a,b)
            z = addOperatorOverloadedFactor(a,b,@mpower,@(z,x,y) z==(x^y));
        end
        
        function z = and(a,b)
            z = addOperatorOverloadedFactor(a,b,@and,@(z,x,y) z == (x&y));
        end
        
        function z = or(a,b)
            z = addOperatorOverloadedFactor(a,b,@or,@(z,x,y) z==(x|y));
        end
        
        function z = mtimes(a,b)
            z = addOperatorOverloadedFactor(a,b,@mtimes,@(z,x,y) z == (x*y));
        end
        
        function z = xor(a,b)
            z = addOperatorOverloadedFactor(a,b,@xor,@(z,x,y) z == xor(x,y));
        end
        
        function z = plus(a,b)
            z = addOperatorOverloadedFactor(a,b,@plus,@(z,x,y) z == (x+y));
        end
        
        function disp(obj)
            disp(obj.Label);
        end
        
        function update(obj)
            obj.VectorObject.update();
        end
        
        function updateEdge(obj,portOrFactor)
            var = obj.getSingleNode();
            
            if isa(portOrFactor,'Factor')
                portNum = var.getPortNum(portOrFactor.IFactor);
                var.updateEdge(portNum);
            else
                obj.VectorObject.updateEdge(portOrFactor-1);
            end
        end
        
        function x = get.Domain(obj)
            x = obj.Domain;
        end
        
        
        function guess = get.Guess(obj)
            guess = wrapNames(obj.VectorObject.getGuess());
        end
        function set.Guess(obj,guess)
            if obj.VectorObject.size() > 1
                error('only support one variable right now');
            end
            obj.VectorObject.setGuess(guess);
        end
        
        
        function factors = get.Factors(obj)
            factors = obj.getFactors(intmax);
        end
        
        function factors = get.FactorsTop(obj)
            factors = obj.getFactors(0);
        end
        
        function factors = get.FactorsFlat(obj)
            factors = obj.getFactors(intmax);
        end
        
        function factors = getFactors(obj,relativeNestingDepth)
            tmp = cell(obj.VectorObject.getFactors(relativeNestingDepth));
            factors = cell(size(tmp));
            for i = 1:length(factors)
                if tmp{i}.isGraph()
                    factors{i} = FactorGraph('igraph',tmp{i});
                elseif tmp{i}.isDiscrete()
                    factors{i} = DiscreteFactor(tmp{i});
                else
                    factors{i} = Factor(tmp{i});
                end
            end
        end
        
    end
    
    methods (Access = protected)
        
        
        
        function z = addOperatorOverloadedFactor(a,b,operation,factor)
            domaina = a.Domain.Elements;
            domainb = b.Domain.Elements;
            zdomain = zeros(length(domaina)*length(domainb),1);
            
            curIndex = 1;
            
            for i = 1:length(domaina)
                for j = 1:length(domainb)
                    zdomain(curIndex) = operation(domaina{i},domainb{j});
                    curIndex = curIndex+1;
                end
            end
            
            zdomain = unique(zdomain);
            zdomain = sort(zdomain);
            
            %znested = Variable(zdomain);
            z = Variable(zdomain);
            
            %Eventually can build combo table, right now, this will do
            fg = getFactorGraph();
            
            fg.addFactor(factor,z,a,b);
            
        end
        
        %{
function x = checkMatchAndCreateVariable(obj,args,v_all)
            v = args{1};
            for i = 2:length(args)
                if v.Solver ~= args{i}.Solver
                    error('Solvers dont match');
                end
            end
            x = v.createVariable(v_all);
        end
        %}
        %{
function varids = getVarIds(obj)
            v = obj.V;
            varids = zeros(numel(v),1);
            for i = 1:numel(v)
                varids(i) = v(i).VarId;
            end
        end
        %}
        
        
        
        function b = getDomain(obj)
            b = obj.Domain;
        end
        
    end
    %{
    methods (Abstract, Access = public)
        createVariable(obj,domain,varMat,indices)
    end
    %}
    
    methods (Access=protected)
        function verifyCanConcatenate(obj,otherObjects)
            for i = 1:length(otherObjects)
                if ~isa(otherObjects{i},'VariableBase')
                    error('Only variables can be concatentated with other variables');
                end
                if ~isequal(obj.Domain,otherObjects{i}.Domain)
                    error('Domains must match when concatenating');
                end
            end
        end
    end
    
end
