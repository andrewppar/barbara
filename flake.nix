{
  description = "Barbara: ⚡CLI for Tunnelblick"  ;
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable" ;
    flake-utils.url = "github:numtide/flake-utils";
  } ;
  outputs = { self, nixpkgs, flake-utils, ... } :
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system ; };
        cljs = pkgs.fetchurl {
          hash = "sha256-2xPFeMvpvErBL2KFbbcx2iMXENovsrk+3bubntp78tc=" ;
          url = "https://github.com/clojure/clojurescript/releases/download/r1.11.60/cljs.jar";
        } ;
        buildSteps =
          [
            ''export HOME=$PWD/src ''
            ''java -cp ${cljs}:src cljs.main -co build.edn -O advanced -c''
            ''sed -i '1s/^/#!\/usr\/bin\/env osascript -l JavaScript\n\n /' ./out/main.js''
            ''chmod +x ./out/main.js''
          ] ;
        installSteps =
          [
            ''mkdir -p $out''
            ''cp ./out/main.js $out/barbara''
          ] ;
        buildDependencies = [pkgs.clojure pkgs.openjdk] ;
      in rec {
        packages.default = pkgs.stdenv.mkDerivation rec {
          name = "barabara" ;
          version = "0.0.1" ;
          src = ./. ;
          buildInputs = buildDependencies ;
          buildPhase = builtins.concatStringsSep "\n" buildSteps ;
          installPhase = builtins.concatStringsSep "\n" installSteps ;
        };
        devShells.default =
          let
            shell-fn = {name, commands}:
              "function " + name + " () {\n" +
              (builtins.foldl' (acc: elem: acc + " " + elem + "\n") "" commands)
              + "}\n" ;
            fns = builtins.concatStringsSep "\n"
              [
                (shell-fn {name = "build"; commands = buildSteps;})
                (shell-fn {name = "install" ; commands = installSteps;})
                (shell-fn {name = "setup" ; commands = [ "build" "install"];})
                (shell-fn {name = "run" ; commands =  ["$out/barbara $@"];})
                (shell-fn {name = "all" ; commands = [ "setup" "run"];})


              ] ;
          in
            pkgs.mkShell {
              packages = buildDependencies ;
              shellHook = fns + '' echo "mac <3 clojurescript"'' ;
            } ;
      }) ;
}
