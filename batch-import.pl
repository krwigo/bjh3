#!/usr/bin/perl
# 2021.04.02

$home = "/pub/workspace";
$proj = "$home/bjh3";

sub cmd {
	print "\$ @_\n";
	system(@_);
}

# git clone
chdir $home;
if (!-d "bjh3") {
	cmd("git", "clone", "https://github.com/krwigo/bjh3.git");
	print "*** edit .gitignore\n";
	exit;
}

# git reset
chdir $proj;
cmd("git", "reset", "--hard", "origin");
foreach $branch (split /[\r\n]+/, `git branch -l`) {
	$branch =~ s/(^[^a-z]+|[^a-z]+$)//ig;
	cmd("git", "branch", "-d", $branch, "--force") if $branch ne "main";
}

chdir $home;
foreach $zip (sort <B*zip>) {
	chdir $home;
	($ts, $y, $m, $d) = $zip =~ /^bjh3-((\d{4})(\d{2})(\d{2}))/i;
	$ds = "$y.$m.$d";
	print "\@ $zip ($ts) ($ds)\n";
	die if !$ts;

	# git branch
	chdir $proj;
	cmd("git", "checkout", "-b", $zip);

	# unzip
	chdir $home;
	cmd("unzip", "-qo", $zip, "-d", "bjh3/");

	# git clear tags
	# chdir $proj;
	# foreach $tag (split /[\r\n]+/, `git tag -l`) {
	#	cmd("git", "tag", "-d", $tag);
	# }

	# git add tags
	# chdir $proj;
	# cmd("git", "tag", "-s", $ds, "-m", "");
	# cmd("git", "tag", "-s", $zip, "-m", "");

	# git add files
	chdir $proj;
	cmd("git", "add", ".");
	# cmd("git", "status");

	# git commit
	chdir $proj;
	cmd("git", "checkout", "main");
	cmd("git", "merge", $zip);
	cmd("git", "commit", "--date", $ds, "-m", "import $zip");
	# cmd("git", "clean", "-f");
}

# cmd("git", "remote", "set-url", "origin", "git\@github.com:krwigo/bjh3.git");
# cmd("git", "push");
