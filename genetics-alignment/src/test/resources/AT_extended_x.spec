% Feature Specification v1.0

FEATURES
consonantal   con    binary    %  0
sonorant      son    binary    %  1
continuant    cnt    binary    %  2
ejective      eje    binary    %  3
release       rel    binary    %  4
lateral       lat    binary    %  5
nasal         nas    binary    %  6
labial        lab    binary    %  7
round         rnd    binary    %  8
coronal       cor    binary    %  9
anterior      ant    binary    % 10
grooved       grv    binary    % 11
dorsal        dor    binary    % 12
front         frn    binary    % 13
back          bck    binary    % 14
high          hgt    binary    % 15
low           low    binary    % 16
atr           atr    binary    % 17
voice         vce    binary    % 18
creaky        crk    binary    % 19
breathy       brt    binary    % 20
distributed   dst    binary    % 21
long          lng    binary    % 22

ALIASES
[+vot]    = [+breathy]
[-vot]    = [-breathy]

CONSTRAINTS
% [+nas] and [+lat] cannot co-occur
[+nasal]   > [-lateral]
[+lateral] > [-nasal]

% [-con] and [-son] cannot co-occur
[-sonorant]    > [+consonantal]
[-consonantal] > [+sonorant]

% [continuant] only applies to [+consonantal]
[-continuant]  > [+consonantal]
[-consonantal] > [+continuant]

% Segment can only be [+rel] if it is also [+consonantal, -continuant]
[+release]     > [+consonantal]
[+release]     > [-sonorant]

[-consonantal] > [-release]
[+continuant]  > [-release]
[+sonorant]    > [-release]

% Only obstruents (stops?) can be ejectives
[+ejective] > [+consonantal, -sonorant, -continuant, -voice]

[+sonorant]    > [-ejective]
[-consonantal] > [-ejective]
[+continuant]  > [-ejective]
[+voice]       > [-ejective]

% Distrubuted requires a closure or near closure
[+distributed] > [+consonantal]
[-consonantal] > [-distributed]

% Obstruents default to ATR for no strong reason
[-son] > [-atr]
[+atr] > [+son]

[+creaky]  > [-breathy, +voice]
[+breathy] > [-creaky]
