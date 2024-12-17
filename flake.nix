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
        lock = builtins.fromJSON (builtins.readFile ./deps-lock.json) ;
        consUrl = segments:
          nixpkgs.lib.pipe segments
            [(map (pkgs.lib.strings.removeSuffix "/"))
             (map (pkgs.lib.strings.removePrefix "/"))
             (pkgs.lib.concatStringsSep "/")];
        mkMvnDep = dep :
          let
            drv = pkgs.fetchurl {
              hash = dep.hash ;
              url = consUrl [ dep.mvn-repo dep.mvn-path] ;
            };
          in { path = drv ; name = dep.mvn-path ;} ;
        deps = map mkMvnDep lock.mvn-deps ;
        #depsClassPath = builtins.concatStringsSep ":" (map (x: "${x}") deps) ;
        depsCache = pkgs.linkFarm "mvn-cache" deps ;

        cljs = pkgs.fetchurl {
          hash = "sha256-2xPFeMvpvErBL2KFbbcx2iMXENovsrk+3bubntp78tc=" ;
          url = "https://github.com/clojure/clojurescript/releases/download/r1.11.60/cljs.jar";
        } ;

        cljs-jar = builtins.baseNameOf cljs ;

        tbc = pkgs.stdenv.mkDerivation rec {
          name = "barabara" ;
          version = "0.0.1" ;
          src = ./. ;
          buildInputs = [pkgs.clojure pkgs.openjdk] ;
          buildPhase =
            builtins.concatStringsSep "\n"
              [
                ''export HOME=$PWD/src ''
                ''java -cp ${cljs}:src cljs.main -co build.edn -O advanced -c''
                ''sed -i '1s/^/#!\/usr\/bin\/env osascript -l JavaScript\n\n /' ./out/main.js''
                ''chmod +x ./out/main.js''
              ] ;
          installPhase =
            builtins.concatStringsSep "\n"
              [ ''mkdir -p $out''
                ''cp ./out/main.js $out/barbara''] ;
        };
      in rec {
        defaultPackage = tbc ;

      }) ;
}
