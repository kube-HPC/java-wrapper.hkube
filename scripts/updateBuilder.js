
const { Octokit } = require("@octokit/rest");


const ownerRepo = {
    owner: 'kube-hpc',
    repo: 'hkube'
};

const main = async () => {
    const version = process.env.revision+process.env.suffix;
    const file = 'core/algorithm-builder/environments/java/wrapper/version.txt';
    const octokit = new Octokit({ auth: process.env.GH_TOKEN });
    const packageJsonContentResponse = await octokit.repos.getContent({
        ...ownerRepo,
        path: file
    });
    const versionFile = Buffer.from(packageJsonContentResponse.data.content, 'base64').toString('utf-8');

    const versionFileSha = packageJsonContentResponse.data.sha;
    // update package json
    const newContent = version.toString('base64');

    const masterShaResponse = await octokit.repos.listCommits({
        ...ownerRepo,
        per_page: 1
    });
    const masterSha = masterShaResponse.data[0].sha;
    const branchName = `update_java_wraper_to_${version.replace('.', '_')}`;
    await octokit.git.createRef({
        ...ownerRepo,
        ref: `refs/heads/${branchName}`,
        sha: masterSha
    });
    await octokit.repos.createOrUpdateFileContents({
        ...ownerRepo,
        path: file,
        message: `update nodejs wrapper to ${version}`,
        branch: branchName,
        sha: versionFileSha,
        content: newContent
    });

    await octokit.pulls.create({
        ...ownerRepo,
        title: `update nodejs wrapper to ${version}`,
        head: branchName,
        base: 'master'
    });

};

main();
