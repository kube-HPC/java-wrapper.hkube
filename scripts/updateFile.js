
const { Octokit } = require("@octokit/rest");


const ownerRepo = {
    owner: 'kube-hpc',
    repo: process.env.repository
};

const main = async () => {
    const value = process.env.fileContent;
    const file = process.env.updatedFilePath;
    const octokit = new Octokit({ auth: process.env.GH_TOKEN });
    const packageJsonContentResponse = await octokit.repos.getContent({
        ...ownerRepo,
        path: file
    });
    const valueFile = Buffer.from(packageJsonContentResponse.data.content, 'base64').toString('utf-8');

    const valueFileSha = packageJsonContentResponse.data.sha;
    // update package json
    const newContent = Buffer.from(value).toString('base64');

    const masterShaResponse = await octokit.repos.listCommits({
        ...ownerRepo,
        per_page: 1
    });
    const masterSha = masterShaResponse.data[0].sha;
    const branchName = `update_java_wraper_to_${value.replace('.', '_')}`;
    await octokit.git.createRef({
        ...ownerRepo,
        ref: `refs/heads/${branchName}`,
        sha: masterSha
    });
    await octokit.repos.createOrUpdateFileContents({
        ...ownerRepo,
        path: file,
        message: `update ${file} to ${value}`,
        branch: branchName,
        sha: valueFileSha,
        content: newContent
    });

    await octokit.pulls.create({
        ...ownerRepo,
        title: `update java wrapper to ${value}`,
        head: branchName,
        base: `${process.env.targetBranch}`
    });

};

main();
