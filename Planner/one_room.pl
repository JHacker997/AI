
%%%%%%%%% Simple Prolog Planner %%%%%%%%%%%%%%%%%%%%%%%%%%
%%%
%%% Based on one of the sample programs in:
%%%
%%% Artificial Intelligence:
%%% Structures and strategies for complex problem solving
%%%
%%% by George F. Luger and William A. Stubblefield
%%%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
:- module( planner,
	   [
	       plan/4,change_state/3,conditions_met/2,member_state/2,
	       move/3,go/2,test/0,test2/0
	   ]).

:- [utils].

plan(State, Goal, _, Moves) :-	equal_set(State, Goal),
				write('moves ar'), nl,
				reverse_print_stack(Moves).
plan(State, Goal, Been_list, Moves) :-
				move(Name, Preconditions, Actions),
				conditions_met(Preconditions, State),
				change_state(State, Actions, Child_state),
				not(member_state(Child_state, Been_list)),
				stack(Child_state, Been_list, New_been_list),
				stack(Name, Moves, New_moves),
			plan(Child_state, Goal, New_been_list, New_moves),!.

change_state(S, [], S).
change_state(S, [add(P)|T], S_new) :-	change_state(S, T, S2),
					add_to_set(P, S2, S_new), !.
change_state(S, [del(P)|T], S_new) :-	change_state(S, T, S2),
					remove_from_set(P, S2, S_new), !.
conditions_met(P, S) :- subset(P, S).

member_state(S, [H|_]) :-	equal_set(S, H).
member_state(S, [_|T]) :-	member_state(S, T).

/* move types */

move(pickup(X), [handempty, clear(X), on(X, Y)],
		[del(handempty), del(clear(X)), del(on(X, Y)),
				 add(clear(Y)),	add(holding(X))]).

move(pickup(X), [handempty, clear(X), ontable(X)],
		[del(handempty), del(clear(X)), del(ontable(X)),
				 add(holding(X))]).

move(putdown(X), [holding(X)],
		[del(holding(X)), add(ontable(X)), add(clear(X)),
				  add(handempty)]).

move(stack(X, Y), [holding(X), clear(Y)],
		[del(holding(X)), del(clear(Y)), add(handempty), add(on(X, Y)),
				  add(clear(X))]).
/* run commands */

go(S, G) :- plan(S, G, [S], []).

test :- go([handempty, ontable(b), ontable(c), on(a, b), clear(c), clear(a)],
	          [handempty, ontable(c), on(a,b), on(b, c), clear(a)]).

test2 :- go([handempty, ontable(b), ontable(c), on(a, b), clear(c), clear(a)],
	          [handempty, ontable(a), ontable(b), on(c, b), clear(a), clear(c)]).
