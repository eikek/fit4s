lib: rec {
  version = "0.10.0-SNAPSHOT";

  latest-release = "0.9.0";

  license = lib.licenses.gpl3;
  homepage = https://github.com/eikek/fit4s;

  meta-bin = {
    description = ''
      Scala library for reading FIT files.
    '';

    inherit license homepage;
  };

  meta-src = {
    description = ''
      Scala library for reading FIT files.
    '';

    inherit license homepage;
  };
}
