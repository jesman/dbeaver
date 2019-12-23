/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreBackupRestoreSettings;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseRestoreInfo;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreDatabaseRestoreSettings;
import org.jkiss.dbeaver.ext.postgresql.tasks.PostgreSQLTasks;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class PostgreRestoreWizard extends PostgreBackupRestoreWizard<PostgreDatabaseRestoreSettings, PostgreDatabaseRestoreInfo> implements IExportWizard {

    private PostgreRestoreWizardPageSettings settingsPage;

    PostgreRestoreWizard(DBTTask task) {
        super(task);
    }

    PostgreRestoreWizard(PostgreDatabase database) {
        super(Collections.singletonList(database), PostgreMessages.wizard_restore_title);
        getSettings().setRestoreInfo(new PostgreDatabaseRestoreInfo(database));
    }

    @Override
    public String getTaskTypeId() {
        return PostgreSQLTasks.TASK_DATABASE_RESTORE;
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, Map<String, Object> state) {
        settingsPage.saveState();

        getSettings().saveSettings(runnableContext, new TaskPreferenceStore(state));
    }

    @Override
    public boolean isExportWizard() {
        return false;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        settingsPage = new PostgreRestoreWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addTaskConfigPages();
        addPage(settingsPage);
        super.addPages();
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == settingsPage) {
            return null;
        }
        return super.getNextPage(page);
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        if (page == logPage) {
            return settingsPage;
        }
        return super.getPreviousPage(page);
    }

    @Override
    public void onSuccess(long workTime) {
        UIUtils.showMessageBox(
            getShell(),
            "Database restore",
            "Restore '" + getObjectsName() + "'",
            SWT.ICON_INFORMATION);
    }

    @Override
    public void fillProcessParameters(List<String> cmd, PostgreDatabaseRestoreInfo arg) throws IOException {
        super.fillProcessParameters(cmd, arg);

        if (getSettings().isCleanFirst()) {
            cmd.add("-c");
        }
    }

    @Override
    protected List<String> getCommandLine(PostgreDatabaseRestoreInfo arg) throws IOException {
        List<String> cmd = super.getCommandLine(arg);
        PostgreDatabaseRestoreSettings settings = getSettings();
        if (settings.getFormat() != PostgreBackupRestoreSettings.ExportFormat.PLAIN) {
            cmd.add("--format=" + settings.getFormat().getId());
        }
        cmd.add("--dbname=" + arg.getDatabase().getName());
        if (settings.getFormat() == PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            cmd.add(settings.getInputFile());
        }

        return cmd;
    }

    @Override
    protected PostgreDatabaseRestoreSettings createSettings() {
        return new PostgreDatabaseRestoreSettings();
    }

    @Override
    public Collection<PostgreDatabaseRestoreInfo> getRunInfo() {
        return Collections.singleton(getSettings().getRestoreInfo());
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final PostgreDatabaseRestoreInfo arg, ProcessBuilder processBuilder, Process process) {
        super.startProcessHandler(monitor, arg, processBuilder, process);
        if (getSettings().getFormat() != PostgreBackupRestoreSettings.ExportFormat.DIRECTORY) {
            new BinaryFileTransformerJob(monitor, new File(getSettings().getInputFile()), process.getOutputStream()).start();
        }
    }

}
